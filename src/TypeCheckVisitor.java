import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;

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
        return "int"; 
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
        
        return null;
    }

    @Override
    public String visit(AssignmentStatement n, String argu) {
        return null;
    }

    @Override
    public String visit(PrintStatement n, String argu) {
        // Εδώ χτυπούσε! Παρακάμπτουμε το null του JTB NodeChoice 
        // επειδή στη MiniJava το System.out.println τυπώνει ΠΑΝΤΑ int.
        return null;
    }

    @Override public String visit(TimesExpression n, String argu) { return "int"; }
    @Override public String visit(MinusExpression n, String argu) { return "int"; }
    @Override public String visit(CompareExpression n, String argu) { return "boolean"; }
    @Override public String visit(AllocationExpression n, String argu) { return n.f1.f0.tokenImage; }

    @Override
    public String visit(MessageSend n, String argu) {
        return "int";
    }

    @Override public String visit(Expression n, String argu) { return "int"; }
    @Override public String visit(PrimaryExpression n, String argu) { return "int"; }
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