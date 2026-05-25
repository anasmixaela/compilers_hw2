import java.io.*;
import java.util.*;
import syntaxtree.*;

public class Main {

    public static void main(String[] args) {

        // check if input files are provided
        if (args.length == 0) {
            System.out.println("Usage: java Main <file1> <file2> ...");
            return;
        }

        MiniJavaParser parser = null;

        // iterate over all input files
        for (String file : args) {
            try (FileInputStream fis = new FileInputStream(file)) {

                // initialize parser once or reinitialize for next file
                if (parser == null) {
                    parser = new MiniJavaParser(fis);
                } else {
                    parser.ReInit(fis);
                }

                // parse input file into AST root
                Goal root = parser.Goal();

                // run symbol collection visitor to build symbol table
                MyVisitor collector = new MyVisitor();
                root.accept(collector, "");

                // retrieve constructed symbol table
                SymbolTable st = collector.st;

                // validate inheritance relations between classes
                st.validateInheritance();

                // run semantic type checking phase
                TypeCheckVisitor typeChecker = new TypeCheckVisitor(st);
                root.accept(typeChecker, "");

                // print offsets for current file
                System.out.println("----------- Offsets for " + file + " -----------");

                for (ClassInfo ci : st.classes.values()) {

                    // print field offsets for current class
                    for (Map.Entry<String, Integer> entry : ci.fieldOffsets.entrySet()) {
                        System.out.println(entry.getKey() + " : " + entry.getValue());
                    }

                    // print method offsets for current class
                    for (Map.Entry<String, Integer> entry : ci.methodOffsets.entrySet()) {

                        String fullMethodName = entry.getKey();

                        // skip main method because it is not part of vtable layout
                        if (fullMethodName.contains(".main")) {
                            continue;
                        }

                        // check if method is overridden in superclass
                        boolean isOverride = false;
                        String parentName = ci.parent;

                        while (parentName != null) {

                            ClassInfo parentClass = st.classes.get(parentName);

                            if (parentClass == null) {
                                break;
                            }

                            // method exists in parent class so it is an override
                            if (parentClass.methodOffsets.containsKey(fullMethodName)) {
                                isOverride = true;
                                break;
                            }

                            parentName = parentClass.parent;
                        }

                        // print only methods that are not overrides
                        if (!isOverride) {
                            System.out.println(fullMethodName + " : " + entry.getValue());
                        }
                    }
                }

                // separator between files output
                System.out.println();

            } catch (Exception e) {
                // print compilation or semantic error for this file
                System.err.println("Error in file " + file + ": " + e.getMessage());
            }
        }
    }
}