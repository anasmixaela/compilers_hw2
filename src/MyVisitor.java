import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.util.*;

public class MyVisitor extends GJNoArguDepthFirst<String> {
    public SymbolTable st = new SymbolTable();
    private String currentClass;

    @Override
    public String visit(ClassDeclaration n) {
        String className = n.f1.f0.tokenImage;
        st.addClass(className, null);
        currentClass = className;
        super.visit(n);
        return null;
    }

    @Override
    public String visit(ClassExtendsDeclaration n) {
        String className = n.f1.f0.tokenImage;
        String parentName = n.f3.f0.tokenImage;
        st.addClass(className, parentName);
        currentClass = className;
        super.visit(n);
        return null;
    }

    @Override public String visit(IntegerType n) { return "int"; }
    @Override public String visit(BooleanType n) { return "boolean"; }
    @Override public String visit(ArrayType n) { return "int[]"; }
    @Override public String visit(Identifier n) { return n.f0.tokenImage; }
}