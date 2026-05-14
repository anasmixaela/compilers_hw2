import java.io.*;
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
                Goal root = parser.Goal(); // Ξεκίνα το Parsing
                
                MyVisitor collector = new MyVisitor();
                root.accept(collector); // Γέμισε το Symbol Table
                
                System.out.println("File " + file + " parsed successfully.");
                
            } catch (Exception e) {
                System.err.println("Error in file " + file + ": " + e.getMessage());
            }
        }
    }
}