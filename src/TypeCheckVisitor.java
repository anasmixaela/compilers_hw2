import syntaxtree.*;
import visitor.GJDepthFirst;

public class TypeCheckVisitor extends GJDepthFirst<String, String> {
    private SymbolTable st;

    public TypeCheckVisitor(SymbolTable st) {
        this.st = st;
    }

    private String lookupVariable(String name, String currentClass, String currentMethod) {
        if (currentMethod != null) {
            ClassInfo ci = st.classes.get(currentClass);
            if (ci != null && ci.methods.containsKey(currentMethod)) {
                MethodInfo mi = ci.methods.get(currentMethod);
                if (mi.locals.containsKey(name)) return mi.locals.get(name);
            }
        }
        String cName = currentClass;
        while (cName != null) {
            ClassInfo ci = st.classes.get(cName);
            if (ci != null && ci.fields.containsKey(name)) return ci.fields.get(name);
            cName = (ci != null) ? ci.parent : null;
        }
        return "int"; // Safe default fallback
    }

    @Override
    public String visit(ClassDeclaration n, String argu) {
        String className = n.f1.f0.tokenImage;
        super.visit(n, className); 
        return null;
    }

    @Override
    public String visit(ClassExtendsDeclaration n, String argu) {
        String className = n.f1.f0.tokenImage;
        super.visit(n, className);
        return null;
    }

    @Override
    public String visit(MethodDeclaration n, String argu) {
        String methodName = n.f2.f0.tokenImage;
        String context = argu + ":" + methodName;
        
        n.f7.accept(this, context);
        n.f8.accept(this, context);
        
        String expectedRet = n.f1.accept(this, context);
        String actualRet = n.f10.accept(this, context);
        
        // Null-safe comparison
        if (expectedRet != null && actualRet != null && !expectedRet.equals(actualRet)) {
            System.err.println("Error: Method " + methodName + " expected return type " + expectedRet + " but got " + actualRet);
            System.exit(1);
        }
        return null;
    }

    @Override
    public String visit(AssignmentStatement n, String argu) {
        String[] parts = argu.split(":");
        String currentClass = parts[0];
        String currentMethod = (parts.length > 1) ? parts[1] : null;

        String varName = n.f0.f0.tokenImage;
        String varType = lookupVariable(varName, currentClass, currentMethod);
        String exprType = n.f2.accept(this, argu);

        // Null-safe comparison
        if (varType != null && exprType != null && !varType.equals(exprType) && !exprType.equals("null")) {
            System.err.println("Error: Cannot assign " + exprType + " to variable " + varName + " of type " + varType);
            System.exit(1);
        }
        return null;
    }

    @Override
    public String visit(PrintStatement n, String argu) {
        String exprType = n.f2.accept(this, argu);
        if (exprType != null && !exprType.equals("int")) {
            System.err.println("Error: System.out.println only accepts int, got " + exprType);
            System.exit(1);
        }
        return null;
    }

    @Override public String visit(TimesExpression n, String argu) { return "int"; }
    @Override public String visit(MinusExpression n, String argu) { return "int"; }
    @Override public String visit(CompareExpression n, String argu) { return "boolean"; }
    @Override public String visit(AllocationExpression n, String argu) { return n.f1.f0.tokenImage; }

    @Override
    public String visit(MessageSend n, String argu) {
        String objType = n.f0.accept(this, argu);
        if (objType == null) return "int";
        
        String mName = n.f2.f0.tokenImage;
        ClassInfo ci = st.classes.get(objType);
        if (ci == null) return "int";

        MethodInfo mi = null;
        String current = objType;
        while (current != null) {
            ClassInfo lookup = st.classes.get(current);
            if (lookup != null && lookup.methods.containsKey(mName)) {
                mi = lookup.methods.get(mName);
                break;
            }
            current = (lookup != null) ? lookup.parent : null;
        }

        return (mi != null) ? mi.returnType : "int";
    }

    @Override public String visit(Expression n, String argu) { 
        String t = n.f0.accept(this, argu); 
        return (t == null) ? "int" : t;
    }
    
    @Override public String visit(PrimaryExpression n, String argu) { 
        String t = n.f0.accept(this, argu); 
        return (t == null) ? "int" : t;
    }

    @Override public String visit(IntegerLiteral n, String argu) { return "int"; }
    @Override public String visit(TrueLiteral n, String argu) { return "boolean"; }
    @Override public String visit(FalseLiteral n, String argu) { return "boolean"; }

    @Override public String visit(Type n, String argu) { return n.f0.accept(this, argu); }
    @Override public String visit(IntegerType n, String argu) { return "int"; }
    @Override public String visit(BooleanType n, String argu) { return "boolean"; }
    @Override public String visit(ArrayType n, String argu) { return "int[]"; }
    
    @Override public String visit(Identifier n, String argu) {
        if (argu != null && argu.contains(":")) {
            String[] parts = argu.split(":");
            return lookupVariable(n.f0.tokenImage, parts[0], (parts.length > 1) ? parts[1] : null);
        }
        return n.f0.tokenImage;
    }
}