# Lite C Compiler/Interpreter
A compiler/interpreter for a small subset of C written in Java. The compiler produces x64 assembly as the intermediate bytecode.
Currently supports:
* `int` data type is a 64bit signed integer
* Functions, recursion and call nesting
* Standard math operations: `+`, `-`, `*`, `/`, `%`
* Boolean operations: `&&`, `||`, `!`, `==`, `!=`, `>`, `>=`, `<`, `<=`
* Bitwise operations: `~`, `&`, `|`, `^`, `<<`, `<<`
* Pre/Post Inc/Decrement: `++`, `--` 
* Op= operations: `+=`, `-=`, `*=`, `/=`, `%=`, `|=`, `&=`, `^=`, `<<=`, `>>=`
* Built-in function for printing integers: `print(int x);`
* Flow control: `if`, `else`, `for`, `while`, `break`, `continue`
You can see the provided test_suite.cl for tests of all of the language features

# Usage
You can run the program as a compiler and interpreter, as a compiler only and assemble the x64 bytecode or run the interpreter on previously written x64 assembly. Use the `-h` switch to see all of the options.

## Compiler
To use compiler use the `-c` switch. If you do not provide an output file with `-o` then compiler will produce `<filename>.asm` as the output to the same folder. You can then use an assembler to assemble the program. For windows you can use `nasm` and `golink`. For the text program you can use the following commands in Windows:
```
litecc -c test.cl
nasm -f win64 test_suite.asm
golink /entry:main /console kernel32.dll msvcrt.dll test_suite.obj
test_suite.exe
```

## Interpreter
The interpreter is run with `-i` switch. The interpreter features:
* Only supports the x64 instructions that the compiler outputs. 
* Full x64 ABI
* Call the built in function print with `call print` and pass the argument with the first call register for function passing
* The interpreter skips everything thru `SECTION .DATA` and will skip any definition of a built in function

The following instructions are supported:
```
nop
mov
push
pop
call
ret
neg
add
sub
imul
div
inc
dec
not
and
or
xor
shl
shr
cmp
jmp
je
jne
jl
jle
jg
jge
```
