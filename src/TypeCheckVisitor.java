import syntaxtree.*;
import visitor.GJDepthFirst;

public class TypeCheckVisitor extends GJDepthFirst<String, String> {

    // reference to the global symbol table containing all semantic information
    private final SymbolTable st;

    // keeps track of the currently active class during type checking traversal
    private String currentClass = null;

    // keeps track of the currently active method during traversal
    private String currentMethod = null;

    // initializes the type checker with the symbol table built in the previous phase
    public TypeCheckVisitor(SymbolTable st) {
        this.st = st;
    }

    // retrieves metadata for the currently active method in the current class
    // used for resolving local variables and parameter types
    private MethodInfo getCurrentMethodInfo() {

        ClassInfo ci = st.classes.get(currentClass);

        if (ci == null) {
            throw new RuntimeException(
                "Internal error: unknown or missing class context: " + currentClass
            );
        }

        // search for method metadata in current class
        for (MethodInfo m : ci.methods.values()) {
            if (m.name.equals(currentMethod)) {
                return m;
            }
        }

        return null;
    }

    // resolves a variable name to its declared type following scope rules
    // checks local variables first then parameters then class fields
    private String lookupVariable(String name) {

        // check current method scope first if inside a method
        if (currentMethod != null) {

            MethodInfo m = getCurrentMethodInfo();

            if (m != null) {

                // local variable lookup
                if (m.locals.containsKey(name)) {
                    return m.locals.get(name);
                }
            }
        }

        // if not found in method scope search class hierarchy
        String cName = currentClass;

        while (cName != null) {

            ClassInfo ci = st.classes.get(cName);

            if (ci != null &&
                ci.fields.containsKey(name)) {
                return ci.fields.get(name);
            }

            cName = (ci != null) 
                ? ci.parent 
                : null;
        }

        // variable not found in any scope
        return null;
    }

    // searches for a method in a class and its inheritance chain
    // used for method call type checking and dispatch resolution
    private MethodInfo lookupMethod(String className, String methodName) {

        // traverse inheritance hierarchy until method is found or root is reached
        while (className != null) {

            ClassInfo ci =
                st.classes.get(className);

            if (ci == null) {
                break;
            }

            for (MethodInfo m : ci.methods.values()) {
                if (m.name.equals(methodName)) {
                    return m;
                }
            }

            className = ci.parent;
        }

        return null;
    }

    // entry point for type checking traversal- visits main class fisrt then all other classes
    @Override
    public String visit(Goal n, String argu) {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }

    // forwards type checking to the specific statement subtype (assignment, if, while, print , etc.)
    @Override
    public String visit(Statement n, String argu) {
        return n.f0.accept(this, argu);
    }

    // type check expressions and return their evaluated type
    @Override
    public String visit(MainClass n, String argu) {

        currentClass = n.f1.f0.tokenImage;
        currentMethod = "main";

        // type check variable declarations
        n.f14.accept(this, argu);

        // type check statements
        n.f15.accept(this, argu);

        currentMethod = null;

        return null;
    }

    // type check class declarations and their members
    @Override
    public String visit(ClassDeclaration n, String argu) {

        currentClass = n.f1.f0.tokenImage;
        currentMethod = null;

        if (!st.classes.containsKey(currentClass)) {
            throw new RuntimeException("Unknown class: " + currentClass);
        }

        // type check fields and methods
        n.f4.accept(this, argu);

        return null;
    }

    // type check subclass declarations and their members to ensurw inheritance rules are respected
    @Override
    public String visit(ClassExtendsDeclaration n, String argu) {

        currentClass = n.f1.f0.tokenImage;
        currentMethod = null;

        if (!st.classes.containsKey(currentClass)) {
            throw new RuntimeException("Unknown class: " + currentClass);
        }

        // type check fields and methods
        n.f6.accept(this, argu);

        return null;
    }

    // type check method declarations including return type parameter types and method body
    @Override
    public String visit(MethodDeclaration n, String argu) {

        // set current method context
        currentMethod = n.f2.f0.tokenImage;

        // resolve declared return type
        String declaredType =
            n.f1.accept(this, argu);

        // type check local variable declarations
        n.f7.accept(this, argu);

        // type check method statements
        n.f8.accept(this, argu);

        // evaluate actual return expression type
        String returnType = n.f10.accept(this, argu);

        if (returnType == null) {
            throw new RuntimeException("Error: invalid return expression.");
        }

        // enforce return type compatibility
        if (!st.isSubtype(returnType, declaredType)) {
            throw new RuntimeException(
                "Error: return type mismatch in method " + currentMethod
            );
        }

        currentMethod = null;

        return null;
    }

    // type check variable declarations in method bodies and class fields for duplicates and correct type usage
    @Override
    public String visit(AssignmentStatement n, String argu) {

        // resolve variable being assigned
        String varName = n.f0.f0.tokenImage;
        String varType = lookupVariable(varName);

        if (varType == null) {
            throw new RuntimeException(
                "Error: Variable " + varName + " is not declared."
            );
        }

        // evaluate right-hand side expression type
        String exprType = n.f2.accept(this, argu);

        if (exprType == null) {
            throw new RuntimeException(
                "Error: Invalid expression type."
            );
        }

        // enforce assignment compatibility
        if (!st.isSubtype(exprType, varType)) {
            throw new RuntimeException(
                "Error: Cannot assign " +
                exprType +
                " to variable of type " +
                varType
            );
        }

        return null;
    }

    // evaluates and returns the type of the contained expression
    @Override
    public String visit(Expression n, String argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public String visit(ExpressionList n, String argu) {

        // evaluate the first expr in arg list
        String result =
            n.f0.accept(this, argu);

            // evaluate remaining expr if they exist
        String tail =
            n.f1.accept(this, argu);

            // combine all arg types inone string (with commas)
        return (tail == null) 
            ? result 
            : result + tail;
    }

    @Override
    public String visit(ExpressionTail n, String argu) {

        // collecting all additional expressions after the first one in a method cal
        StringBuilder sb =
            new StringBuilder();

        for (int i = 0; i < n.f0.size(); i++) {

            sb.append(
                n.f0.elementAt(i).accept(this, argu)
            );
        }

        return sb.toString();
    }

    @Override
    public String visit(ExpressionTerm n, String argu) {
        // prepping comma to use for arg split
        return "," + n.f1.accept(this, argu);
    }

    @Override
    public String visit(Clause n, String argu) {
        // evaluate contained bool clause expr
        return n.f0.accept(this, argu);
    }

    @Override
    public String visit(Type n, String argu) {
        // forward visit to the specific type node
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
    public String visit(PrimaryExpression n, String argu) {
        // evaluate contained primary expr
        return n.f0.accept(this, argu);
    }

    @Override
    public String visit(PlusExpression n, String argu) {

        // both operants of addition
        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        // both must be int
        if (!"int".equals(left) || !"int".equals(right)) {
            throw new RuntimeException("Error: + operator requires int operands.");
        }

        return "int";
    }

    @Override
    public String visit(MinusExpression n, String argu) {

        // both operants of subtraction
        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        // only ints
        if (!"int".equals(left) || !"int".equals(right)) {
            throw new RuntimeException("Error: - operator requires int operands.");
        }

        return "int";
    }

    @Override
    public String visit(TimesExpression n, String argu) {

        // multiplication operands
        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        // both int
        if (!"int".equals(left) || !"int".equals(right)) {
            throw new RuntimeException("Error: * operator requires int operands.");
        }

        return "int";
    }

    @Override
    public String visit(CompareExpression n, String argu) {

        // comparison operands
        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        // both int
        if (!"int".equals(left) || !"int".equals(right)) {
            throw new RuntimeException("Error: < operator requires int operands.");
        }

        return "boolean";
    }

    // integer literals always evaluate to int
    @Override public String visit(IntegerLiteral n, String argu) { return "int"; }
    // true literal evaluates to boolean
    @Override public String visit(TrueLiteral n, String argu) { return "boolean"; }
    // false literal evaluates to boolean
    @Override public String visit(FalseLiteral n, String argu) { return "boolean"; }

    @Override
    public String visit(ThisExpression n, String argu) {

        //currentclass to become the resulting type
        if (currentClass == null) {
            throw new RuntimeException("Error: invalid use of this.");
        }

        return currentClass;
    }

    @Override
    public String visit(AllocationExpression n, String argu) {

        String className = n.f1.f0.tokenImage;

        // object creation only valid for defined classes
        if (!st.classes.containsKey(className)) {
            throw new RuntimeException("Error: undefined class " + className);
        }

        return className;
    }

    @Override
    public String visit(MessageSend n, String argu) {

        String objType =
            n.f0.accept(this, argu);

        // primitive types and arrays cannot receive method calls
        if ("int".equals(objType) ||
            "boolean".equals(objType) ||
            "int[]".equals(objType)) {

            throw new RuntimeException("Error: message send on non-object.");
        }

        String methodName =
            n.f2.f0.tokenImage;

        // search for method in class hierarchy
        MethodInfo method =
            lookupMethod(objType, methodName);

        // method must exist in object type
        if (method == null) {
            throw new RuntimeException("Error: method " + methodName + " not found.");
        }

        // store evaluated arg types from method call
        java.util.ArrayList<String> argTypes =
            new java.util.ArrayList<>();

        // process args if method call contains any
        if (n.f4.present()) {

            String args =
                n.f4.accept(this, argu);

            // split if comma separated
            if (args != null && !args.isEmpty()) {

                String[] split = args.split(",");

                for (String s : split) {
                    argTypes.add(s.trim());
                }
            }
        }

        // arg count compatibility
        if (argTypes.size() != method.paramTypes.size()) {
            throw new RuntimeException(
                "Error: wrong number of arguments in call to " + methodName
            );
        }

        // verufy each arg type against expected parameter type
        for (int i = 0; i < argTypes.size(); i++) {

            String actual = argTypes.get(i);
            String expected = method.paramTypes.get(i);

            if (!st.isSubtype(actual, expected)) {
                throw new RuntimeException(
                    "Error: argument type mismatch in call to " + methodName
                );
            }
        }

        return method.returnType;
    }

    @Override
    public String visit(ArrayLookup n, String argu) {

        // array index and expression types
        String arrType = n.f0.accept(this, argu);
        String idxType = n.f2.accept(this, argu);

        // left side must be an int array
        if (!arrType.equals("int[]")) {
            throw new RuntimeException("Error: array lookup on non-array type.");
        }

        if (!idxType.equals("int")) {
            throw new RuntimeException("Error: array index must be int.");
        }

        return "int";
    }

    @Override
    public String visit(ArrayLength n, String argu) {

        // evaluate type before .length 
        String arrType = n.f0.accept(this, argu);

        if (!arrType.equals("int[]")) {
            throw new RuntimeException("Error: length applied to non-array.");
        }

        return "int";
    }

    @Override
    public String visit(ArrayAssignmentStatement n, String argu) {

        // retrieve variable type for array being assigned to
        String varType =
            lookupVariable(n.f0.f0.tokenImage);

        // assingment target must be an int array
        if (varType == null || !"int[]".equals(varType)) {
            throw new RuntimeException("Error: array assignment on non-array.");
        }

        // evaluate index expr type
        String indexType =
            n.f2.accept(this, argu);

        if (!"int".equals(indexType)) {
            throw new RuntimeException("Error: array index must be int.");
        }

        // assinged value type
        String exprType = n.f5.accept(this, argu);

        if (!"int".equals(exprType)) {
            throw new RuntimeException("Error: array elements must be int.");
        }

        return null;
    }

    @Override
    public String visit(IfStatement n, String argu) {

        String cond =
            n.f2.accept(this, argu);

        if (!"boolean".equals(cond)) {
            throw new RuntimeException("Error: if condition must be boolean.");
        }

        // type check bith brakches
        n.f4.accept(this, argu);
        n.f6.accept(this, argu);

        return null;
    }

    @Override
    public String visit(WhileStatement n, String argu) {

        String cond =
            n.f2.accept(this, argu);

        if (!"boolean".equals(cond)) {
            throw new RuntimeException("Error: while condition must be boolean.");
        }

        // type check loop body
        n.f4.accept(this, argu);

        return null;
    }

    @Override
    public String visit(PrintStatement n, String argu) {

        String exprType =
            n.f2.accept(this, argu);

        // minijava only allows printing ints
        if (!"int".equals(exprType)) {
            throw new RuntimeException("Error: System.out.println requires int.");
        }

        return null;
    }

    @Override
    public String visit(AndExpression n, String argu) {

        // both loical operands
        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        if (!"boolean".equals(left) ||
            !"boolean".equals(right)) {

            throw new RuntimeException("Error: && operator requires boolean operands.");
        }

        return "boolean";
    }

    @Override
    public String visit(NotExpression n, String argu) {

        // expr after !
        String type = n.f1.accept(this, argu);

        if (!type.equals("boolean")) {
            throw new RuntimeException("Error: ! operator requires boolean.");
        }

        return "boolean";
    }

    @Override
    public String visit(BracketExpression n, String argu) {
        return n.f1.accept(this, argu);
    }

    @Override
    public String visit(ArrayAllocationExpression n, String argu) {

        String sizeType = n.f3.accept(this, argu);

        if (!sizeType.equals("int")) {
            throw new RuntimeException("Error: array size must be int.");
        }

        return "int[]";
    }

    @Override
    public String visit(Identifier n, String argu) {

        // iddentifier name from ast node
        String name = n.f0.tokenImage;

        // only lookup variables if we are inside a class
        if (currentClass != null) {

            String type = lookupVariable(name);

            if (type != null) {
                return type;
            }
        }

        // identfiers may also refer to class names
        if (st.classes.containsKey(name)) {
            return name;
        }

        // no identfier was declared
        throw new RuntimeException("Error: variable " + name + " is not declared.");
    }
}