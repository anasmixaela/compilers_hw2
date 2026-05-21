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
                
                MyVisitor collector = new MyVisitor();
                root.accept(collector); 

                SymbolTable st = collector.st;

                TypeCheckVisitor typeChecker = new TypeCheckVisitor(st);
                root.accept(typeChecker, null);
                
                System.out.println("----------- Offsets for " + file + " -----------");
                
                for (ClassInfo ci : st.classes.values()) {
                    // 1. Εκτύπωση field offsets
                    for (Map.Entry<String, Integer> entry : ci.fieldOffsets.entrySet()) {
                        System.out.println(entry.getKey() + " : " + entry.getValue());
                    }
                    
                    // 2. Εκτύπωση method offsets (Συμπεριλαμβανομένων των overrides!)
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
            }
        }
    }
}