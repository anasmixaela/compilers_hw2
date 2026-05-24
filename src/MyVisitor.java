import syntaxtree.*;
import visitor.GJDepthFirst;

public class MyVisitor extends GJDepthFirst<String, String> {

    // shared symbol table
    public SymbolTable st = new SymbolTable();

    // current class during traversal
    private String currentClass = null;

    // current method during traversal
    private MethodInfo currentMethod = null;

    @Override
    public String visit(ClassDeclaration n, String argu) {

        String className = n.f1.f0.tokenImage;

        // add class without parent
        st.addClass(className, null);

        currentClass = className;
        currentMethod = null;

        super.visit(n, argu);

        return null;
    }

    @Override
    public String visit(
        ClassExtendsDeclaration n,
        String argu
    ) {

        String className = n.f1.f0.tokenImage;
        String parentName = n.f3.f0.tokenImage;

        // add subclass
        st.addClass(className, parentName);

        currentClass = className;
        currentMethod = null;

        ClassInfo child = st.classes.get(className);
        ClassInfo parent = st.classes.get(parentName);

        // inherit offsets from parent
        if (parent != null) {

            child.nextFieldOffset =
                parent.nextFieldOffset;

            child.nextMethodOffset =
                parent.nextMethodOffset;
        }

        super.visit(n, argu);

        return null;
    }

    @Override
    public String visit(VarDeclaration n, String argu) {

        String type = n.f0.accept(this, argu);
        String name = n.f1.f0.tokenImage;

        ClassInfo ci = st.classes.get(currentClass);

        // local variable
        if (currentMethod != null) {

            if (currentMethod.locals.containsKey(name)) {

                throw new RuntimeException("Error: duplicate local variable " + name);
            }

            currentMethod.locals.put(name, type);
        }

        // class field
        else {

            if (ci.fields.containsKey(name)) {

                System.err.println(
                    "Error: duplicate field " + name
                );

                throw new RuntimeException("Error: duplicate field " + name);
            }

            ci.fields.put(name, type);

            // store field offset
            ci.fieldOffsets.put(
                currentClass + "." + name,
                ci.nextFieldOffset
            );

            // advance offset
            ci.nextFieldOffset +=
                st.getTypeSize(type);
        }

        return null;
    }

    @Override
    public String visit(
        MethodDeclaration n,
        String argu
    ) {

        String returnType =
            n.f1.accept(this, argu);

        String methodName =
            n.f2.f0.tokenImage;

        ClassInfo ci =
            st.classes.get(currentClass);

        // create temporary method object
        currentMethod =
            new MethodInfo(
                methodName,
                returnType
            );

        // collect parameters
        n.f4.accept(this, argu);

        String signature =
            currentMethod.getSignature();

        // check duplicate methods
        if (ci.methods.containsKey(signature)) {

            throw new RuntimeException("Error: duplicate method " + signature);
        }

        boolean isOverride = false;
        int inheritedOffset = -1;

        // walk inheritance chain
        String parentName = ci.parent;

        while (parentName != null) {

            ClassInfo parent =
                st.classes.get(parentName);

            if (parent == null) {
                break;
            }

            // compare against parent methods
            for (MethodInfo parentMethod :
                parent.methods.values()) {

                // different names are irrelevant
                if (!parentMethod.name.equals(methodName)) {
                    continue;
                }

                // exact same parameter types
                boolean sameParams =
                    parentMethod.paramTypes.equals(
                        currentMethod.paramTypes
                    );

                // overriding case
                if (sameParams) {

                    isOverride = true;

                    // return type must match
                    if (!parentMethod.returnType.equals(returnType)) {

                        throw new RuntimeException("Error: invalid override of method " + methodName);
                    }

                    Integer offset =
                        parent.methodOffsets.get(
                            parentMethod.getSignature()
                        );

                    if (offset != null) {
                        inheritedOffset = offset;
                    }
                }

                // illegal overload case
                else if (
                    st.methodsConflict(
                        currentMethod,
                        parentMethod
                    )
                ) {

                    throw new RuntimeException("Error: illegal overload for method " + methodName);
                }
            }

            parentName = parent.parent;
        }

        // store override offset
        if (isOverride) {

            ci.methodOffsets.put(
                signature,
                inheritedOffset
            );
        }

        // assign new offset
        else {

            ci.methodOffsets.put(
                signature,
                ci.nextMethodOffset
            );

            ci.nextMethodOffset += 8;
        }

        // insert method into class
        ci.methods.put(signature, currentMethod);

        // visit locals
        n.f7.accept(this, argu);

        // method finished
        currentMethod = null;

        return null;
    }

    @Override
    public String visit(
        FormalParameter n,
        String argu
    ) {

        String type =
            n.f0.accept(this, argu);

        String name =
            n.f1.f0.tokenImage;

        // duplicate parameter
        if (currentMethod.parameters.containsKey(name)) {

            throw new RuntimeException("Error: duplicate parameter " + name);
        }

        // store parameter
        currentMethod.parameters.put(
            name,
            type
        );

        currentMethod.paramTypes.add(type);

        // parameters are visible as locals
        currentMethod.locals.put(
            name,
            type
        );

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