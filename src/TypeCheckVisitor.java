import syntaxtree.*;
import visitor.GJDepthFirst;

public class TypeCheckVisitor extends GJDepthFirst<String, String> {
    private final SymbolTable st;
    private String currentClass = null;
    private String currentMethod = null;

    public TypeCheckVisitor(SymbolTable st) {
        this.st = st;
    }

    private MethodInfo getCurrentMethodInfo() {

        ClassInfo ci = st.classes.get(currentClass);

        if (ci == null) {
            return null;
        }

        for (MethodInfo m : ci.methods.values()) {
            if (m.name.equals(currentMethod)) {
                return m;
            }
        }

        return null;
    }

    private String lookupVariable(String name) {

        if (currentMethod != null) {

            MethodInfo m = getCurrentMethodInfo();

            if (m != null) {

                if (m.locals.containsKey(name)) {
                    return m.locals.get(name);
                }

                if (m.parameters.containsKey(name)) {
                    return m.parameters.get(name);
                }
            }
        }

        String cName = currentClass;

        while (cName != null) {

            ClassInfo ci = st.classes.get(cName);

            if (ci != null &&
                ci.fields.containsKey(name)) {

                return ci.fields.get(name);
            }

            cName = (ci != null) ? ci.parent : null;
        }

        return null;
    }

    // searches method in class hierarchy
    private MethodInfo lookupMethod(
        String className,
        String methodName
    ) {

        // walk inheritance chain
        while (className != null) {

            ClassInfo ci =
                st.classes.get(className);

            if (ci != null) {

                // search methods
                for (MethodInfo m :
                    ci.methods.values()) {

                    // found matching method
                    if (m.name.equals(methodName)) {
                        return m;
                    }
                }

                // continue to parent
                className = ci.parent;
            }
            else {
                className = null;
            }
        }

        return null;
    }

    @Override
    public String visit(Goal n, String argu) {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }

    @Override
    public String visit(Statement n, String argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public String visit(MainClass n, String argu) {

        currentClass = n.f1.f0.tokenImage;
        currentMethod = "main";

        n.f14.accept(this, argu);

        return null;
    }

    @Override
    public String visit(ClassDeclaration n, String argu) {

        // enter class
        currentClass = n.f1.f0.tokenImage;

        currentMethod = null;

        n.f4.accept(this, argu);
        return null;
    }

    @Override
    public String visit(ClassExtendsDeclaration n, String argu) {

        // enter subclass
        currentClass = n.f1.f0.tokenImage;

        currentMethod = null;

        n.f6.accept(this, argu);
        return null;
    }

    @Override
    public String visit(MethodDeclaration n, String argu) {

        currentMethod = n.f2.f0.tokenImage;

        String declaredType =
            n.f1.accept(this, argu);

        if (n.f7.present()) {
            n.f7.accept(this, argu);
        }

        if (n.f8.present()) {
            for (int i = 0; i < n.f8.size(); i++) {
                n.f8.nodes.get(i).accept(this, argu);
            }
        }

        String returnType =
            n.f10.accept(this, argu);

        if (!st.isSubtype(returnType, declaredType)) {

            throw new RuntimeException(
                "Error: return type mismatch in method "
                + currentMethod
            );
        }

        return null;
    }

    @Override
    public String visit(AssignmentStatement n, String argu) {

        String varName = n.f0.f0.tokenImage;
        String varType = lookupVariable(varName);

        if (varType == null) {

            throw new RuntimeException(
                "Error: Variable " + varName + " is not declared."
            );
        }

        String exprType = n.f2.accept(this, argu);

        if (exprType == null) {

            throw new RuntimeException(
                "Error: Invalid expression type."
            );
        }

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

    @Override
    public String visit(Expression n, String argu) {
        return n.f0.accept(this, argu);
    }

    @Override
    public String visit(ExpressionList n, String argu) {

        String result =
            n.f0.accept(this, argu);

        String tail =
            n.f1.accept(this, argu);

        if (tail != null) {
            result += tail;
        }

        return result;
    }

    @Override
    public String visit(ExpressionTail n, String argu) {

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

        return "," +
            n.f1.accept(this, argu);
    }

    @Override
    public String visit(Clause n, String argu) {
        return n.f0.accept(this, argu);
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
    public String visit(PrimaryExpression n, String argu) {

        // just return child expression type
        return n.f0.accept(this, argu);
    }

    @Override
    public String visit(PlusExpression n, String argu) {

        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        if (!"int".equals(left) || !"int".equals(right)) {
            throw new RuntimeException("Error: + operator requires int operands.");
        }

        return "int";
    }

    @Override
    public String visit(MinusExpression n, String argu) {

        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        if (!"int".equals(left) || !"int".equals(right)) {
            throw new RuntimeException("Error: - operator requires int operands.");
        }

        return "int";
    }

    @Override
    public String visit(TimesExpression n, String argu) {

        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        if (!"int".equals(left) || !"int".equals(right)) {
            throw new RuntimeException("Error: * operator requires int operands.");
        }

        return "int";
    }

    @Override
    public String visit(CompareExpression n, String argu) {

        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        if (!"int".equals(left) || !"int".equals(right)) {
            throw new RuntimeException("Error: < operator requires int operands.");
        }

        return "boolean";
    }
    @Override public String visit(IntegerLiteral n, String argu) { return "int"; }
    @Override public String visit(TrueLiteral n, String argu) { return "boolean"; }
    @Override public String visit(FalseLiteral n, String argu) { return "boolean"; }
    
    @Override 
    public String visit(ThisExpression n, String argu) { 
        if (currentClass == null) {
            throw new RuntimeException("Error: invalid use of this.");
        }

        return currentClass;
    }

    @Override 
    public String visit(AllocationExpression n, String argu) { 
        String className = n.f1.f0.tokenImage;

        if (!st.classes.containsKey(className)) {

            throw new RuntimeException("Error: undefined class " + className);
        }

        return className;
    }

    @Override
    public String visit(MessageSend n, String argu) {

        String objType =
            n.f0.accept(this, argu);

        if ("int".equals(objType) ||
            "boolean".equals(objType) ||
            "int[]".equals(objType)) {

            throw new RuntimeException(
                "Error: message send on non-object."
            );
        }

        String methodName =
            n.f2.f0.tokenImage;

        MethodInfo method =
            lookupMethod(objType, methodName);

        if (method == null) {

            throw new RuntimeException(
                "Error: method " +
                methodName +
                " not found."
            );
        }

        // collect actual argument types
        java.util.ArrayList<String> argTypes =
            new java.util.ArrayList<>();

        if (n.f4.present()) {

            String args =
                n.f4.accept(this, argu);

            if (args != null && !args.isEmpty()) {

                String[] split =
                    args.split(",");

                for (String s : split) {
                    argTypes.add(s.trim());
                }
            }
        }

        // check argument count
        if (argTypes.size() != method.paramTypes.size()) {

            throw new RuntimeException(
                "Error: wrong number of arguments in call to "
                + methodName
            );
        }

        // check argument types
        for (int i = 0; i < argTypes.size(); i++) {

            String actual =
                argTypes.get(i);

            String expected =
                method.paramTypes.get(i);

            if (!st.isSubtype(actual, expected)) {

                throw new RuntimeException(
                    "Error: argument type mismatch in call to "
                    + methodName
                );
            }
        }

        return method.returnType;
    }

    @Override
    public String visit(ArrayLookup n, String argu) {

        String arrType = n.f0.accept(this, argu);
        String idxType = n.f2.accept(this, argu);

        if (!arrType.equals("int[]")) {

            throw new RuntimeException(
                "Error: array lookup on non-array type."
            );
        }

        if (!idxType.equals("int")) {

            throw new RuntimeException(
                "Error: array index must be int."
            );
        }

        return "int";
    }

    @Override
    public String visit(ArrayLength n, String argu) {

        String arrType = n.f0.accept(this, argu);

        if (!arrType.equals("int[]")) {

            throw new RuntimeException(
                "Error: length applied to non-array."
            );
        }

        return "int";
    }

    @Override
    public String visit(ArrayAssignmentStatement n, String argu) {

        String varType =
            lookupVariable(n.f0.f0.tokenImage);

        if (varType == null || !"int[]".equals(varType)) {

            throw new RuntimeException(
                "Error: array assignment on non-array."
            );
        }

        String indexType =
            n.f2.accept(this, argu);

        if (!"int".equals(indexType)) {

            throw new RuntimeException(
                "Error: array index must be int."
            );
        }

        String exprType = n.f5.accept(this, argu);

        if (!"int".equals(exprType)) {
            throw new RuntimeException(
                "Error: array elements must be int."
            );
        }
        return null;
    }

    @Override
    public String visit(IfStatement n, String argu) {

        String cond =
            n.f2.accept(this, argu);

        if (!"boolean".equals(cond)) {

            throw new RuntimeException(
                "Error: if condition must be boolean."
            );
        }

        n.f4.accept(this, argu);
        n.f6.accept(this, argu);

        return null;
    }

    @Override
    public String visit(WhileStatement n, String argu) {

        String cond =
            n.f2.accept(this, argu);

        if (!"boolean".equals(cond)) {

            throw new RuntimeException(
                "Error: while condition must be boolean."
            );
        }

        n.f4.accept(this, argu);

        return null;
    }

    @Override
    public String visit(PrintStatement n, String argu) {

        String exprType =
            n.f2.accept(this, argu);

        if (!"int".equals(exprType)) {

            throw new RuntimeException(
                "Error: System.out.println requires int."
            );
        }

        return null;
    }

    @Override
    public String visit(AndExpression n, String argu) {

        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        // both operands must be boolean
        if (!"boolean".equals(left) ||
            !"boolean".equals(right)) {

            throw new RuntimeException(
                "Error: && operator requires boolean operands."
            );
        }

        return "boolean";
    }

    @Override
    public String visit(NotExpression n, String argu) {

        String type = n.f1.accept(this, argu);

        // ! only works on booleans
        if (!type.equals("boolean")) {

            throw new RuntimeException(
                "Error: ! operator requires boolean."
            );
        }

        return "boolean";
    }

    @Override
    public String visit(BracketExpression n, String argu) {

        // brackets do not change type
        return n.f1.accept(this, argu);
    }

    @Override
    public String visit(ArrayAllocationExpression n, String argu) {

        String sizeType = n.f3.accept(this, argu);

        // array size must be int
        if (!sizeType.equals("int")) {

            throw new RuntimeException(
                "Error: array size must be int."
            );
        }

        return "int[]";
    }

    @Override
    public String visit(Identifier n, String argu) {

        // get identifier name
        String name = n.f0.tokenImage;

        // search variable type
        String type = lookupVariable(name);

        // variable found
        if (type != null) {
            return type;
        }

        // maybe identifier is a class name
        if (st.classes.containsKey(name)) {
            return name;
        }

        throw new RuntimeException(
            "Error: variable " + name + " is not declared."
        );
    }
}