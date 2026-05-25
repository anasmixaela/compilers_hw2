import syntaxtree.*;
import visitor.GJDepthFirst;

public class MyVisitor extends GJDepthFirst<String, String> {

    // global symbol table shared across the whole compilation phase
    // it stores all classes, methods, fields and offset information
    public SymbolTable st = new SymbolTable();

    // keeps track of the currently visited class during AST traversal
    // used to correctly assign fields and methods to the right class
    private String currentClass = null;

    // keeps track of the currently visited method
    // used to distinguish between class fields and local variables
    private MethodInfo currentMethod = null;

    @Override
    public String visit(MainClass n, String argu) {

        String className = n.f1.f0.tokenImage;

        st.addClass(className, null);

        currentClass = className;

        ClassInfo ci = st.classes.get(className);

        currentMethod = new MethodInfo("main", "void");

        // add String[] args parameter
        String argName = n.f11.f0.tokenImage;

        currentMethod.parameters.put(argName, "String[]");
        currentMethod.locals.put(argName, "String[]");
        currentMethod.paramTypes.add("String[]");

        ci.methods.put("main()", currentMethod);

        // visit variable declarations inside main
        n.f14.accept(this, argu);

        // visit statements inside main
        n.f15.accept(this, argu);

        currentMethod = null;

        return null;
    }

    @Override
    public String visit(ClassDeclaration n, String argu) {

        // extract class name from AST node
        String className = n.f1.f0.tokenImage;

        // register class in symbol table without parent (no inheritance case)
        st.addClass(className, null);

        // update current context to this class
        currentClass = className;

        // no method context at class level
        currentMethod = null;

        // continue traversal of class body
        super.visit(n, argu);

        return null;
    }

    @Override
    public String visit(ClassExtendsDeclaration n, String argu) {

        // extract subclass and parent class names
        String className = n.f1.f0.tokenImage;
        String parentName = n.f3.f0.tokenImage;

        if (!st.classes.containsKey(parentName)) {
            throw new RuntimeException(
                "Error: unknown parent class " + parentName
            );
        }

        // register subclass with inheritance relationship
        st.addClass(className, parentName);

        // update traversal context
        currentClass = className;
        currentMethod = null;

        ClassInfo child = st.classes.get(className);
        ClassInfo parent = st.classes.get(parentName);

        if (parent == null) {
            throw new RuntimeException(
                "Internal error: parent class not registered: " + parentName
            );
        }

        // inherit offset counters from parent class
        // ensures correct memory layout for fields and methods
        if (parent != null) {

            child.nextFieldOffset =
                parent.nextFieldOffset;

            child.nextMethodOffset =
                parent.nextMethodOffset;
        }

        // continue visiting subtree
        super.visit(n, argu);

        return null;
    }

    @Override
    public String visit(VarDeclaration n, String argu) {

        // resolve variable type and name from AST
        String type = n.f0.accept(this, argu);
        String name = n.f1.f0.tokenImage;

        ClassInfo ci = st.classes.get(currentClass);

        // case 1: variable is a local variable inside a method
        if (currentMethod != null) {

            // detect duplicate local declarations
            if (currentMethod.locals.containsKey(name) ||
                currentMethod.parameters.containsKey(name)) {

                throw new RuntimeException(
                    "Error: duplicate local variable " + name
                );
            }

            // store local variable in current method scope
            currentMethod.locals.put(name, type);
        }

        // case 2: variable is a class field
        else {

            // detect duplicate field declarations
            if (ci.fields.containsKey(name)) {
                throw new RuntimeException("Error: duplicate field " + name);
            }

            // store field type in class metadata
            ci.fields.put(name, type);

            // assign memory offset for field access
            ci.fieldOffsets.put(
                currentClass + "." + name,
                ci.nextFieldOffset
            );

            // increase offset based on type size
            ci.nextFieldOffset += st.getTypeSize(type);

        }

        return null;
    }

    @Override
    public String visit(MethodDeclaration n, String argu) {

        // extract return type and method name
        String returnType = n.f1.accept(this, argu);
        String methodName = n.f2.f0.tokenImage;

        ClassInfo ci = st.classes.get(currentClass);

        // create method representation for current method
        currentMethod = new MethodInfo(methodName, returnType);

        // visit formal parameters of method
        n.f4.accept(this, argu);

        // generate unique signature for method
        String signature = currentMethod.getSignature();

        // check for duplicate method definitions in same class
        if (ci.methods.containsKey(signature)) {
            throw new RuntimeException("Error: duplicate method " + signature);
        }

        // MiniJava does not support overloading inside same class
        for (MethodInfo existing : ci.methods.values()) {

            if (existing.name.equals(methodName) &&
                !existing.paramTypes.equals(currentMethod.paramTypes)) {

                throw new RuntimeException(
                    "Error: illegal overload for method " + methodName
                );
            }
        }

        boolean isOverride = false;
        int inheritedOffset = -1;

        // traverse inheritance chain to detect overrides or conflicts
        String parentName = ci.parent;

        while (parentName != null) {

            ClassInfo parent = st.classes.get(parentName);

            if (parent == null) {
                break;
            }

            // compare against all parent methods
            for (MethodInfo parentMethod : parent.methods.values()) {

                // skip unrelated methods
                if (!parentMethod.name.equals(methodName)) {
                    continue;
                }

                // check exact parameter match (true override case)
                boolean sameParams =
                    parentMethod.paramTypes.equals(currentMethod.paramTypes);

                if (sameParams) {

                    // mark override case
                    isOverride = true;

                    // enforce same return type in overrides
                    if (!parentMethod.returnType.equals(returnType)) {
                        throw new RuntimeException("Error: invalid override of method " + methodName);
                    }

                    // reuse parent's method offset
                    Integer offset =
                        parent.methodOffsets.get(parentMethod.getSignature());

                    if (offset != null) {
                        inheritedOffset = offset;
                    }
                }

                // detect illegal overload conflicts
                else if (st.methodsConflict(currentMethod, parentMethod)) {

                    throw new RuntimeException("Error: illegal overload for method " + methodName);
                }
            }

            parentName = parent.parent;
        }

        // assign method offset depending on override status
        if (isOverride) {

            ci.methodOffsets.put(signature, inheritedOffset);

        } else {

            ci.methodOffsets.put(signature, ci.nextMethodOffset);

            // each method assumed to occupy fixed size in vtable
            ci.nextMethodOffset += 8;
        }

        // store method metadata in class
        ci.methods.put(signature, currentMethod);

        // visit method body (locals, statements, etc)
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);

        // reset method context after finishing traversal
        currentMethod = null;

        return null;
    }

    @Override
    public String visit(FormalParameter n, String argu) {

        // extract parameter type and name
        String type = n.f0.accept(this, argu);
        String name = n.f1.f0.tokenImage;

        // detect duplicate parameter names
        if (currentMethod.parameters.containsKey(name)) {
            throw new RuntimeException("Error: duplicate parameter " + name);
        }

        // store parameter metadata
        currentMethod.parameters.put(name, type);
        currentMethod.paramTypes.add(type);

        // parameters are visible as locals
        currentMethod.locals.put(name, type);

        return null;
    }

    @Override
    public String visit(Type n, String argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public String visit(IntegerType n, String argu) {
        return "int";
    }

    @Override
    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    @Override
    public String visit(ArrayType n, String argu) {
        return "int[]";
    }

    @Override
    public String visit(Identifier n, String argu) {
        return n.f0.tokenImage;
    }
}