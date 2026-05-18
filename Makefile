# compile everything
default:
	javac src/*.java src/syntaxtree/*.java src/visitor/*.java

# clean up .class files if needed
clean:
	rm -f src/*.class src/syntaxtree/*.class src/visitor/*.class

# execute the compiler with the factorial example directly from windows path
run-factorial: default
	java -cp src Main /mnt/d/diiiit/6th/compilers/compilers_hw2/minijava-examples-new/Factorial.java

# execute all new examples at once (as requested in question 8)
run-all: default
	java -cp src Main /mnt/d/diiiit/6th/compilers/compilers_hw2/minijava-examples-new/*.java