import java.io.*;
import java.util.*;
import syntaxtree.*;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java Main <file1> <file2> ...");
            return;
        }

        MiniJavaParser parser = null;

        for (String file : args) {
            try (FileInputStream fis = new FileInputStream(file)) {
                if (parser == null) {
                    parser = new MiniJavaParser(fis);
                } else {
                    parser.ReInit(fis);
                }

                Goal root = parser.Goal(); 
                
                // collect symbols using standard void visitor
                MyVisitor collector = new MyVisitor();
                root.accept(collector); 

                // get the populated symbol table
                SymbolTable st = collector.st;
                st.validateInheritance();

                // perform semantic type checking
                TypeCheckVisitor typeChecker = new TypeCheckVisitor(st);
                root.accept(typeChecker, ""); 
                
                System.out.println("----------- Offsets for " + file + " -----------");
                
                for (ClassInfo ci : st.classes.values()) {
                    // print field offsets
                    for (Map.Entry<String, Integer> entry : ci.fieldOffsets.entrySet()) {
                        System.out.println(entry.getKey() + " : " + entry.getValue());
                    }
                    
                    // print method offsets including overrides
                    for (Map.Entry<String, Integer> entry : ci.methodOffsets.entrySet()) {
                        String fullMethodName = entry.getKey();
                        String methodName = fullMethodName.substring(fullMethodName.indexOf(".") + 1);
                        
                        if (!methodName.equals("main")) {
                            System.out.println(fullMethodName + " : " + entry.getValue());
                        }
                    }
                }
                System.out.println();
                
            } catch (Exception e) {
                System.err.println("Error in file " + file + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}