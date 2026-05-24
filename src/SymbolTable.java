import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

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

    public boolean isSubtype(String child, String parent) {
        if (child == null || parent == null) {
            return false;
        }
        if (child.equals(parent)) {
            return true;
        }

        ClassInfo ci = classes.get(child);

        while (ci != null) {
            if (ci.parent != null && ci.parent.equals(parent)) {
                return true;
            }
            ci = classes.get(ci.parent);
        }

        return false;
    }

    public void validateInheritance() {
        for (ClassInfo ci : classes.values()) {
            if (ci.parent != null && !classes.containsKey(ci.parent)) {
                System.err.println(
                    "Error: Class " + ci.name +
                    " extends undefined class " + ci.parent
                );
                System.exit(1);
            }

            String slow = ci.name;
            String fast = ci.parent;

            while (fast != null) {
                ClassInfo fastInfo = classes.get(fast);
                if (fastInfo == null) {
                    break;
                }
                fast = fastInfo.parent;
                if (fast != null) {
                    ClassInfo secondHop = classes.get(fast);
                    if (secondHop != null) {
                        fast = secondHop.parent;
                    }
                }

                ClassInfo slowInfo = classes.get(slow);

                if (slowInfo != null) {
                    slow = slowInfo.parent;
                }
                if (slow != null && slow.equals(fast)) {
                    System.err.println(
                        "Error: Cyclic inheritance involving class " + ci.name
                    );
                    System.exit(1);
                }
            }
        }
    }
}

class ClassInfo { 
    public String name;
    public String parent;
    public Map<String, String> fields = new LinkedHashMap<>();
    public Map<String, List<MethodInfo>> methods = new LinkedHashMap<>();

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
    // use explicit java.util.List to bypass any awt conflicts
    public List<String> paramTypes = new ArrayList<>();
    public LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

    // local variables map
    public Map<String, String> locals = new LinkedHashMap<>(); 

    public MethodInfo(String name, String returnType) {
        this.name = name;
        this.returnType = returnType;
    }
}