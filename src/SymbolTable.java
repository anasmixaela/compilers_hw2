import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {

    // stores all program classes
    Map<String, ClassInfo> classes = new LinkedHashMap<>();

    // add new class to symbol table
    public void addClass(String name, String parent) {

        if (classes.containsKey(name)) {

            throw new RuntimeException("Error: duplicate class " + name);
        }

        classes.put(name, new ClassInfo(name, parent));
    }

    // returns field size in bytes
    public int getTypeSize(String type) {

        if (type.equals("int")) {
            return 4;
        }

        if (type.equals("boolean")) {
            return 1;
        }

        // arrays and objects are pointers
        return 8;
    }

    // checks inheritance relationship
    public boolean isSubtype(String child, String parent) {

        if (child == null || parent == null) {
            return false;
        }

        // same type
        if (child.equals(parent)) {
            return true;
        }

        // walk inheritance chain
        ClassInfo current = classes.get(child);

        while (current != null) {

            if (current.parent != null &&
                current.parent.equals(parent)) {

                return true;
            }

            current = classes.get(current.parent);
        }

        return false;
    }

    // checks if two methods create illegal overload
    boolean methodsConflict(
        MethodInfo a,
        MethodInfo b
    ) {

        // different names are always ok
        if (!a.name.equals(b.name)) {
            return false;
        }

        // different argument count is always ok
        if (a.paramTypes.size() != b.paramTypes.size()) {
            return false;
        }

        // check if all positions are comparable
        for (int i = 0; i < a.paramTypes.size(); i++) {

            String typeA = a.paramTypes.get(i);
            String typeB = b.paramTypes.get(i);

            boolean comparable =
                isSubtype(typeA, typeB) ||
                isSubtype(typeB, typeA);

            // one unrelated position means legal overload
            if (!comparable) {
                return false;
            }
        }

        // all positions comparable -> illegal overlap
        return true;
    }

    // validates inheritance graph
    public void validateInheritance() {

        for (ClassInfo ci : classes.values()) {

            // parent must exist
            if (ci.parent != null &&
                !classes.containsKey(ci.parent)) {

                throw new RuntimeException("Error: unknown parent class " + ci.parent);
            }

            // detect inheritance cycles
            String current = ci.parent;

            while (current != null) {

                if (current.equals(ci.name)) {

                    throw new RuntimeException("Error: cyclic inheritance");
                }

                ClassInfo parentInfo = classes.get(current);

                if (parentInfo == null) {
                    break;
                }

                current = parentInfo.parent;
            }
        }
    }
}

// stores information for one class
class ClassInfo {

    // class name
    public String name;

    // parent class name
    public String parent;

    // field name -> type
    public LinkedHashMap<String, String> fields =
        new LinkedHashMap<>();

    // method signature -> method info
    public LinkedHashMap<String, MethodInfo> methods =
        new LinkedHashMap<>();

    // field offsets
    public LinkedHashMap<String, Integer> fieldOffsets =
        new LinkedHashMap<>();

    // method offsets
    public LinkedHashMap<String, Integer> methodOffsets =
        new LinkedHashMap<>();

    // next available field offset
    public int nextFieldOffset = 0;

    // next available method offset
    public int nextMethodOffset = 0;

    public ClassInfo(String name, String parent) {

        this.name = name;
        this.parent = parent;
    }
}

// stores information for one method
class MethodInfo {

    // method name
    public String name;

    // method return type
    public String returnType;

    // ordered parameter types
    public ArrayList<String> paramTypes =
        new ArrayList<>();

    // parameter name -> type
    public LinkedHashMap<String, String> parameters =
        new LinkedHashMap<>();

    // local variable name -> type
    public LinkedHashMap<String, String> locals =
        new LinkedHashMap<>();

    public MethodInfo(
        String name,
        String returnType
    ) {

        this.name = name;
        this.returnType = returnType;
    }

    // creates unique method signature
    public String getSignature() {

        StringBuilder sb = new StringBuilder();

        sb.append(name);
        sb.append("(");

        for (int i = 0; i < paramTypes.size(); i++) {

            sb.append(paramTypes.get(i));

            if (i != paramTypes.size() - 1) {
                sb.append(",");
            }
        }

        sb.append(")");

        return sb.toString();
    }
}