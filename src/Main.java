import java.io.*;
import java.util.*;
import syntaxtree.*;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java Main <file1> <file2> ...");
            return;
        }

        for (String file : args) {
            try (FileInputStream fis = new FileInputStream(file)) {
                MiniJavaParser parser = new MiniJavaParser(fis);
                Goal root = parser.Goal(); 
                
                MyVisitor collector = new MyVisitor();
                root.accept(collector); 
                
                System.out.println("----------- Offsets for " + file + " -----------");
                SymbolTable st = collector.st;
                
                for (ClassInfo ci : st.classes.values()) {
                    // print field offsets
                    for (Map.Entry<String, Integer> entry : ci.fieldOffsets.entrySet()) {
                        System.out.println(entry.getKey() + " : " + entry.getValue());
                    }
                    
                    // print method offsets
                    for (Map.Entry<String, Integer> entry : ci.methodOffsets.entrySet()) {
                        String fullMethodName = entry.getKey();
                        String methodName = fullMethodName.substring(fullMethodName.indexOf(".") + 1);
                        
                        // check if method was inherited from any parent
                        boolean isOverride = false;
                        String pName = ci.parent;
                        while (pName != null) {
                            ClassInfo pi = st.classes.get(pName);
                            if (pi != null && pi.methods.containsKey(methodName)) {
                                isOverride = true;
                                break;
                            }
                            pName = (pi != null) ? pi.parent : null;
                        }
                        
                        // skip main and overridden methods
                        if (!isOverride && !methodName.equals("main")) {
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