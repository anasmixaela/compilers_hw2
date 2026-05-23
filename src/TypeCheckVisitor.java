import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;

public class TypeCheckVisitor extends GJDepthFirst<String, String> {
    private final SymbolTable st;
    private String currentClass = null;
    private String currentMethod = null;

    public TypeCheckVisitor(SymbolTable st) {
        this.st = st;
    }

    private String lookupVariable(String name) {
        if (currentMethod != null && currentClass != null) {
            ClassInfo ci = st.classes.get(currentClass);

            if (ci != null && ci.methods.containsKey(currentMethod)) {

                List<MethodInfo> methodList = ci.methods.get(currentMethod);

                if (methodList != null && !methodList.isEmpty()) {

                    MethodInfo mi = methodList.get(0);

                    if (mi.locals.containsKey(name)) {
                        return mi.locals.get(name);
                    }
                }
            }
        }
        String cName = currentClass;
        while (cName != null) {
            ClassInfo ci = st.classes.get(cName);
            if (ci != null && ci.fields.containsKey(name)) return ci.fields.get(name);
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
        if (n.f8.present()) {
            for (int i = 0; i < n.f8.size(); i++) {
                n.f8.nodes.get(i).accept(this, argu);
            }
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
            System.err.println("Error: Variable " + varName + " is not declared.");
            System.exit(1);
        }

        String exprType = n.f2.accept(this, argu);
        
        if (exprType != null && !exprType.equals("int") && !exprType.equals("boolean") && !exprType.equals("int[]")) {
            if (lookupVariable(exprType) == null && !st.classes.containsKey(exprType)) {
                System.err.println("Error: Symbol " + exprType + " not found.");
                System.exit(1);
            }
        }
        return null;
    }

    @Override public String visit(Expression n, String argu) { return n.f0.accept(this, argu); }
    @Override public String visit(PrimaryExpression n, String argu) { return n.f0.accept(this, argu); }

    @Override 
    public String visit(Identifier n, String argu) {
        String varType = lookupVariable(n.f0.tokenImage);
        if (varType != null) return varType;
        return n.f0.tokenImage;
    }

    @Override public String visit(TimesExpression n, String argu) { return "int"; }
    @Override public String visit(MinusExpression n, String argu) { return "int"; }
    @Override public String visit(PlusExpression n, String argu) { return "int"; }
    @Override public String visit(CompareExpression n, String argu) { return "boolean"; }
    @Override public String visit(IntegerLiteral n, String argu) { return "int"; }
    @Override public String visit(TrueLiteral n, String argu) { return "boolean"; }
    @Override public String visit(FalseLiteral n, String argu) { return "boolean"; }
    
    @Override 
    public String visit(ThisExpression n, String argu) { 
        return currentClass != null ? currentClass : "int";
    }

    @Override 
    public String visit(AllocationExpression n, String argu) { 
        return n.f1.f0.tokenImage; 
    }

    @Override
    public String visit(MessageSend n, String argu) {
        String objType = n.f0.accept(this, argu);
        if (objType == null || objType.equals("int") || objType.equals("boolean") || objType.equals("int[]")) return "int";
        
        String mName = n.f2.f0.tokenImage;
        ClassInfo ci = st.classes.get(objType);
        if (ci == null) return "int";

        String current = objType;
        while (current != null) {
            ClassInfo lookup = st.classes.get(current);
            if (lookup != null && lookup.methods.containsKey(mName)) {
                List<MethodInfo> methods = lookup.methods.get(mName);

                if (methods != null && !methods.isEmpty()) {
                    return methods.get(0).returnType;
                }
            }
            current = (lookup != null) ? lookup.parent : null;
        }
        return "int";
    }
}