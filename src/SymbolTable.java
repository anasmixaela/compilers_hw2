import java.util.*;

public class SymbolTable {
    public Map<String, ClassInfo> classes = new LinkedHashMap<>();

    public void addClass(String name, String parent) {
        if (classes.containsKey(name)) {
            System.err.println("Error: Class " + name + " already defined.");
            System.exit(1);
        }
        classes.put(name, new ClassInfo(name, parent));
    }
}

class ClassInfo { 
    public String name;
    public String parent;
    public Map<String, String> fields = new LinkedHashMap<>();
    public Map<String, MethodInfo> methods = new LinkedHashMap<>();
    
    public ClassInfo(String name, String parent) {
        this.name = name;
        this.parent = parent;
    }
}

class MethodInfo {
    public String name;
    public String returnType;
    public List<String> params = new ArrayList<>();
    public Map<String, String> locals = new LinkedHashMap<>();

    public MethodInfo(String name, String returnType) {
        this.name = name;
        this.returnType = returnType;
    }
}