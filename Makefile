# compile everything
default:
	javac src/*.java src/syntaxtree/*.java src/visitor/*.java

# clean up .class files if needed
clean:
	rm -f src/*.class src/syntaxtree/*.class src/visitor/*.class

# run a single test (Factorial)
run-factorial: default
	java -cp src Main official-tests/minijava-examples-new/Factorial.java

# execute all new valid examples at once
run-all: default
	java -cp src Main official-tests/minijava-examples-new/*.java