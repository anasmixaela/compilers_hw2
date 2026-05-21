import syntaxtree.*;
import visitor.GJNoArguDepthFirst;

public class MyVisitor extends GJNoArguDepthFirst<String> {
    public SymbolTable st = new SymbolTable();
    private String currentClass = null;
    private MethodInfo currentMethod = null;

    @Override
    public String visit(ClassDeclaration n) {
        String className = n.f1.f0.tokenImage;
        st.addClass(className, null);
        currentClass = className;
        currentMethod = null;
        super.visit(n);
        return null;
    }

    @Override
    public String visit(ClassExtendsDeclaration n) {
        String className = n.f1.f0.tokenImage;
        String parentName = n.f3.f0.tokenImage;
        st.addClass(className, parentName);
        currentClass = className;
        currentMethod = null;
        
        // inherit starting offsets from parent if parent is already parsed
        ClassInfo child = st.classes.get(className);
        ClassInfo parent = st.classes.get(parentName);
        if (parent != null) {
            child.nextFieldOffset = parent.nextFieldOffset;
            child.nextMethodOffset = parent.nextMethodOffset;
        }
        
        super.visit(n);
        return null;
    }

    @Override
    public String visit(VarDeclaration n) {
        String type = n.f0.accept(this);
        String name = n.f1.f0.tokenImage;

        ClassInfo ci = st.classes.get(currentClass);

        if (currentMethod != null) {
            if (currentMethod.locals.containsKey(name) || currentMethod.params.contains(name)) {
                System.err.println("Error: Duplicate local variable " + name);
                System.exit(1);
            }
            currentMethod.locals.put(name, type);
        } else {
            if (ci.fields.containsKey(name)) {
                System.err.println("Error: Duplicate field " + name);
                System.exit(1);
            }
            ci.fields.put(name, type);
            
            ci.fieldOffsets.put(currentClass + "." + name, ci.nextFieldOffset);
            ci.nextFieldOffset += st.getTypeSize(type);
        }
        return null;
    }

    @Override
    public String visit(MethodDeclaration n) {
        String retType = n.f1.accept(this);
        String name = n.f2.f0.tokenImage;

        ClassInfo ci = st.classes.get(currentClass);
        currentMethod = new MethodInfo(name, retType);
        
        n.f4.accept(this);

        boolean isOverride = false;
        String pName = ci.parent;
        int overrideOffset = -1;

        while (pName != null) {
            ClassInfo pi = st.classes.get(pName);
            if (pi != null && pi.methods.containsKey(name)) {
                MethodInfo pm = pi.methods.get(name);
                // Null-safe checks for overriding
                if (pm.returnType != null && retType != null && pm.returnType.equals(retType) && pm.params.equals(currentMethod.params)) {
                    isOverride = true;
                    Integer pOffset = pi.methodOffsets.get(pName + "." + name);
                    overrideOffset = (pOffset != null) ? pOffset : 0;
                    break;
                }
            }
            pName = (pi != null) ? pi.parent : null;
        }

        ci.methods.put(name, currentMethod);

        if (isOverride) {
            ci.methodOffsets.put(currentClass + "." + name, overrideOffset);
        } else {
            ci.methodOffsets.put(currentClass + "." + name, ci.nextMethodOffset);
            ci.nextMethodOffset += 8;
        }

        n.f7.accept(this);
        n.f8.accept(this);

        currentMethod = null;
        return null;
    }

    @Override
    public String visit(FormalParameter n) {
        String type = n.f0.accept(this);
        String name = n.f1.f0.tokenImage;
        
        if (currentMethod != null) {
            currentMethod.params.add(type);
            currentMethod.locals.put(name, type); 
        }
        return null;
    }

    @Override public String visit(Type n) { return n.f0.accept(this); } // CRITICAL FIX
    @Override public String visit(IntegerType n) { return "int"; }
    @Override public String visit(BooleanType n) { return "boolean"; }
    @Override public String visit(ArrayType n) { return "int[]"; }
    @Override public String visit(Identifier n) { return n.f0.tokenImage; }
}