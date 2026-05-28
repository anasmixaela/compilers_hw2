# compilers_hw2# K31 Compilers — HW2

# Anastopoulou Michaela
# 1115202300007

Compile:
```bash
make
```
Run non error tests:
```bash
make run-extra
```

Run error tests:
```bash
make run-errors
```

Run specific file (e.g.):
```bash
make run-test FILE=official-tests/minijava-examples-new/minijava-extra/Add.java
```

Clean:
```bash
make clean
```

# Implementation #

This project is a MiniJava compiler frontend that performs:

- Symbol table construction
- Semantic analysis (type checking)
- Inheritance validation
- Method and field offset computation (for memory layout / vtables)

The implementation is based on visitor patterns and operates in multiple passes over the AST.

## Compilation Phases

The compiler works in three main phases:

### 1. Symbol Table Construction (`MyVisitor`)
- Collects all classes, fields, and methods.
- Builds a global symbol table (`SymbolTable`).
- Records inheritance relationships between classes.
- Assigns memory offsets for:
  - Class fields
  - Methods (vtable layout)

Each class is stored as a `ClassInfo` object.

Each method is stored as a `MethodInfo` object.

### 2. Inheritance Validation
Performed inside `SymbolTable.validateInheritance()`:

- Checks that every parent class exists.
- Detects cyclic inheritance.
- Ensures the class hierarchy is valid before type checking.


### 3. Type Checking (`TypeCheckVisitor`)
- Ensures all expressions are type correct.
- Validates assignments, method calls, and control structures.
- Checks method return types.
- Ensures subtype compatibility for assignments and parameters.



## Memory Layout (Offsets)

The compiler computes offsets for:

### Fields
- Each field is assigned a byte offset.
- `int = 4 bytes`, `boolean = 1 byte`, references = `8 bytes`.

### Methods
- Each method gets a vtable offset.
- Overridden methods reuse the parent's offset.
- New methods get a new offset (`+8` per method).

## Method Handling Rules

- Method overloading is NOT allowed.
- Method overriding is allowed only if:
  - Same parameter types
  - Same return type
- Otherwise, compilation error is thrown.

## Variable Scoping Rules

Variable lookup follows this order:

1. Local variables inside method
2. Method parameters
3. Class fields
4. Parent classes (for inheritance)

## Error Handling

The compiler detects and reports:

- Duplicate classes
- Duplicate fields or methods
- Illegal overloading
- Invalid method overriding
- Type mismatches
- Undeclared variables
- Invalid array usage

Each error stops compilation for the specific file only.