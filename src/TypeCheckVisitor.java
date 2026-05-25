import syntaxtree.*;
import visitor.GJDepthFirst;

public class TypeCheckVisitor extends GJDepthFirst<String, String> {
    private final SymbolTable st;
    private String currentClass = null;
    private String currentMethod = null;

    public TypeCheckVisitor(SymbolTable st) {
        this.st = st;
    }

    private String lookupVariable(String name) {

        // check locals first
        if (currentMethod != null) {

            ClassInfo ci = st.classes.get(currentClass);

            if (ci != null) {

                for (MethodInfo m : ci.methods.values()) {

                    if (m.name.equals(currentMethod)) {

                        if (m.locals.containsKey(name)) {
                            return m.locals.get(name);
                        }

                        break;
                    }
                }
            }
        }

        // check fields (inheritance chain)
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

    @Override
    public String visit(Goal n, String argu) {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }

    @Override
    public String visit(MainClass n, String argu) {
        currentClass = n.f1.f0.tokenImage;
        currentMethod = "main";
        if (n.f14.present()) {
            for (int i = 0; i < n.f14.size(); i++) {
                n.f14.nodes.get(i).accept(this, argu);
            }
        }
        return null;
    }

    @Override
    public String visit(ClassDeclaration n, String argu) {
        currentClass = n.f1.f0.tokenImage;
        currentMethod = null;
        if (n.f4.present()) {
            for (int i = 0; i < n.f4.size(); i++) {
                n.f4.nodes.get(i).accept(this, argu);
            }
        }
        return null;
    }

    @Override
    public String visit(ClassExtendsDeclaration n, String argu) {
        currentClass = n.f1.f0.tokenImage;
        currentMethod = null;
        if (n.f6.present()) {
            for (int i = 0; i < n.f6.size(); i++) {
                n.f6.nodes.get(i).accept(this, argu);
            }
        }
        return null;
    }

    @Override
    public String visit(MethodDeclaration n, String argu) {

        currentMethod = n.f2.f0.tokenImage;

        String declaredType =
            n.f1.accept(this, argu);

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
    public String visit(Statement n, String argu) {
        n.f0.accept(this, argu);
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
    public String visit(PrimaryExpression n, String argu) {
        String t = n.f0.accept(this, argu);
        return t;
    }

    @Override
    public String visit(Identifier n, String argu) {

        String name = n.f0.tokenImage;

        String varType = lookupVariable(name);

        if (varType != null) {
            return varType;
        }

        throw new RuntimeException(
            "Error: Variable " + name + " is not declared."
        );
    }

    @Override
    public String visit(PlusExpression n, String argu) {

        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        if (left == null || right == null ||
            !"int".equals(left) || !"int".equals(right)) {
            throw new RuntimeException("Error: + operator requires int operands.");
        }

        return "int";
    }

    @Override
    public String visit(MinusExpression n, String argu) {

        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        if (left == null || right == null ||
            !"int".equals(left) || !"int".equals(right)) {
            throw new RuntimeException("Error: - operator requires int operands.");
        }

        return "int";
    }

    @Override
    public String visit(TimesExpression n, String argu) {

        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        if (left == null || right == null ||
            !"int".equals(left) || !"int".equals(right)) {
            throw new RuntimeException("Error: * operator requires int operands.");
        }

        return "int";
    }

    @Override
    public String visit(CompareExpression n, String argu) {

        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        if (left == null || right == null ||
            !"int".equals(left) || !"int".equals(right)) {
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

        String objType = n.f0.accept(this, argu);

        if (objType == null ||
            objType.equals("int") ||
            objType.equals("boolean") ||
            objType.equals("int[]")) {
            throw new RuntimeException(
                "Error: message send on non-object type."
            );
        }

        String mName = n.f2.f0.tokenImage;

        String current = objType;

        while (current != null) {

            ClassInfo lookup = st.classes.get(current);

            if (lookup != null) {

                for (MethodInfo m : lookup.methods.values()) {

                    if (m.name.equals(mName)) {
                        return m.returnType;
                    }
                }
            }

            current = (lookup != null) ? lookup.parent : null;
        }

        throw new RuntimeException("Error: method " + mName + " not found in class " + objType);
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

        if (!cond.equals("boolean")) {

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

        if (!cond.equals("boolean")) {

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

        if (!exprType.equals("int")) {

            throw new RuntimeException(
                "Error: System.out.println requires int."
            );
        }

        return null;
    }
}