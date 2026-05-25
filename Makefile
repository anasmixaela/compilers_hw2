# compile everything
default:
	javac -d out src/*.java src/syntaxtree/*.java src/visitor/*.java

# clean
clean:
	rm -rf out

# run single test
run-test: default
	java -cp out Main $(FILE)

# run all extra tests
run-extra: default
	for f in official-tests/minijava-examples-new/minijava-extra/*.java; do \
		echo "Running $$f"; \
		java -cp out Main "$$f"; \
	done

# run all error tests
run-errors: default
	for f in official-tests/minijava-examples-new/minijava-error-extra/*.java; do \
		echo "Running $$f"; \
		java -cp out Main "$$f"; \
	done