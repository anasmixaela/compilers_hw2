import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {

    // main symbol table structure that stores all classes found in the program
    // uses linkedhashmap to preserve insertion order for deterministic output
    Map<String, ClassInfo> classes = new LinkedHashMap<>();

    // adds a new class entry to the symbol table
    // also checks for duplicate class definitions to avoid semantic errors
    public void addClass(String name, String parent) {

        // prevent redefinition of an already declared class
        if (classes.containsKey(name)) {

            throw new RuntimeException("Error: duplicate class " + name);
        }

        // create and store class metadata including inheritance info
        classes.put(name, new ClassInfo(name, parent));
    }

    // returns memory size in bytes for a given type
    // used during offset computation for fields and parameters
    public int getTypeSize(String type) {

        // integer type occupies 4 bytes
        if (type.equals("int")) {
            return 4;
        }

        // boolean type occupies 1 byte
        if (type.equals("boolean")) {
            return 1;
        }

        // object references and arrays are treated as pointers
        return 8;
    }

    // checks whether child type can be assigned to parent type
    // implements basic inheritance-based subtype checking
    public boolean isSubtype(String child, String parent) {

        // null types are considered invalid
        if (child == null || parent == null) {
            return false;
        }

        // identical types are always compatible
        if (child.equals(parent)) {
            return true;
        }

        // primitive and array types do not participate in inheritance
        if (child.equals("int") ||
            child.equals("boolean") ||
            child.equals("int[]") ||
            child.equals("String[]") ||
            parent.equals("int") ||
            parent.equals("boolean") ||
            parent.equals("int[]") ||
            parent.equals("String[]")) {

            return false;
        }

        // unknown classes are invalid
        if (!classes.containsKey(child) ||
            !classes.containsKey(parent)) {

            return false;
        }

        // walk up the inheritance chain to find compatibility
        ClassInfo current = classes.get(child);

        while (current != null) {

            // if direct parent matches target type, it is a subtype
            if (current.parent != null &&
                current.parent.equals(parent)) {

                return true;
            }

            // move one level up in the hierarchy
            current = classes.get(current.parent);
        }

        // no match found in inheritance chain
        return false;
    }

    // checks whether two methods conflict under overloading rules
    // used to detect illegal method redeclarations
    boolean methodsConflict(
        MethodInfo a,
        MethodInfo b
    ) {

        // miniJava does not support overloading
        // any same-name method with different signature is illegal

        return a.name.equals(b.name);

    }

    // validates the entire inheritance graph for correctness
    // ensures no missing parents and no cyclic inheritance
    public void validateInheritance() {

        // iterate through all declared classes
        for (ClassInfo ci : classes.values()) {

            // ensure parent class exists in symbol table
            if (ci.parent != null &&
                !classes.containsKey(ci.parent)) {

                throw new RuntimeException("Error: unknown parent class " + ci.parent);
            }

            // detect inheritance cycles by walking up parent chain
            String current = ci.parent;

            while (current != null) {

                // if we return to original class then cycle exists
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

// holds all semantic information about a single class
class ClassInfo {

    // name of the class
    public String name;

    // name of parent class if inheritance exists
    public String parent;

    // maps field names to their types
    public LinkedHashMap<String, String> fields =
        new LinkedHashMap<>();

    // maps method signatures to method metadata
    public LinkedHashMap<String, MethodInfo> methods =
        new LinkedHashMap<>();

    // computed memory offsets for fields
    public LinkedHashMap<String, Integer> fieldOffsets =
        new LinkedHashMap<>();

    // computed memory offsets for methods
    public LinkedHashMap<String, Integer> methodOffsets =
        new LinkedHashMap<>();

    // tracks next available offset for fields
    public int nextFieldOffset = 0;

    // tracks next available offset for methods
    public int nextMethodOffset = 0;

    public ClassInfo(String name, String parent) {

        this.name = name;
        this.parent = parent;
    }
}

// holds all semantic information about a single method
class MethodInfo {

    // method identifier (without parameters)
    public String name;

    // return type of the method
    public String returnType;

    // ordered list of parameter types for signature checking
    public ArrayList<String> paramTypes =
        new ArrayList<>();

    // maps parameter names to their types
    public LinkedHashMap<String, String> parameters =
        new LinkedHashMap<>();

    // maps local variable names to their types
    public LinkedHashMap<String, String> locals =
        new LinkedHashMap<>();

    public MethodInfo(
        String name,
        String returnType
    ) {

        this.name = name;
        this.returnType = returnType;
    }

    // builds a unique method signature string used for comparison
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