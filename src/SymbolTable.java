import java.util.*;

public class SymbolTable {
    // map for storing class definitions
    public Map<String, ClassInfo> classes = new LinkedHashMap<>();

    public void addClass(String name, String parent) {
        if (classes.containsKey(name)) {
            System.err.println("Error: Class " + name + " already defined.");
            System.exit(1);
        }
        classes.put(name, new ClassInfo(name, parent));
    }

    // return type size in bytes
    public int getTypeSize(String type) {
        if (type.equals("int")) return 4;
        if (type.equals("boolean")) return 1;
        return 8; // arrays and object pointers
    }
}

class ClassInfo { 
    public String name;
    public String parent;
    public Map<String, String> fields = new LinkedHashMap<>();
    public Map<String, MethodInfo> methods = new LinkedHashMap<>();
    
    // tracks variable offsets
    public Map<String, Integer> fieldOffsets = new LinkedHashMap<>();
    // tracks method offsets
    public Map<String, Integer> methodOffsets = new LinkedHashMap<>();
    
    // next available positions
    public int nextFieldOffset = 0;
    public int nextMethodOffset = 0;
    
    public ClassInfo(String name, String parent) {
        this.name = name;
        this.parent = parent;
    }
}

class MethodInfo {
    public String name;
    public String returnType;
    // explicit java.util.List to avoid compilation errors
    public java.util.List<String> params = new ArrayList<>(); 
    // local variables map
    public Map<String, String> locals = new LinkedHashMap<>(); 

    public MethodInfo(String name, String returnType) {
        this.name = name;
        this.returnType = returnType;
    }
}