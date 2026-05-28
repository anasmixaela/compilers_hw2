# K31 Compilers — HW2

# Anastopoulou Michaela
# 1115202300007


## How to:

Compile:
```bash
make
```
Run correct tests:
```bash
make run-extra
```

Run error tests:
```bash
make run-errors
```

Run a specific file (e.g.):
```bash
make run-test FILE=official-tests/minijava-examples-new/minijava-extra/Add.java
```

Clean build files:
```bash
make clean
```


# Implementation #

This project is a MiniJava compiler frontend that performs:

- Symbol table construction
- Semantic analysis (type checking)
- Inheritance validation
- Method and field offset computation (for memory layout / vtables)

It is implemented using thw Visitor pattern and it works in multiple passes over the AST. It also uses ordered maps to keep the output consistent.


## Compilation Phases

The compiler works in three main phases:

### 1. Symbol Table Construction (`MyVisitor`)
First, it collects all classes, fields, and methods.
It also stores inheritance relations (who extends who).
During this step, it also prepares memory offsets.

### 2. Inheritance Validation
Performed inside `SymbolTable.validateInheritance()`:
Checks that every parent class exists and that there are no cycles in inheritance

### 3. Type Checking (`TypeCheckVisitor`)
Lastly, it ensures all expressions are type correct, it validates assignments, method calls, and control structures and checks method return types.


## Memory Layout (Offsets)

The compiler computes offsets for:

### Fields
Each field is assigned a memory offset:
`int = 4 bytes`, `boolean = 1 byte`, `objects = 8 bytes`.

### Methods
Each method gets a position in the vtable:
New methods get a new slot and overridden methods reuse the parent's offset.


## Method Handling Rules

The rules in handling the methods are:
- No method overloading allowed
- Method overriding is allowed only if parameter types match and return type also match.
- Everything else is rejected with an error


## Variables

When searching for a variable, the compiler looks in thiw order:
1. Local variables inside method
2. Method parameters
3. Class fields
4. Parent classes (inheritance)


## Error Handling

The compiler detects and reports:
- Duplicate classes, fields or methods
- Illegal overloading
- Invalid method overriding
- Type mismatches
- Undeclared variables
- Invalid array usage

Each error stops compilation for the specific file only.