# Lite C Compiler/Interpreter
A compiler/interpreter for a small subset of C written in Java. The compiler produces x64 assembly as the intermediate bytecode.
Currently supports:
* 'int' data type is a 64bit signed integer
* Functions and function nesting (no recursion)
* Basic math operations: add, subtract, multiply, divide and modulus
* Built-in function for printing integers: print(int x)
For an example see test.cl in the root folder

# Usage
You can run the program as a compiler and interpreter, as a compiler only and assemble the x64 bytecode or run the interpreter on previously written x64 assembly.

##Comiler
To use compiler use the '-c' switch. If you do not provide an output file with '-o' then compiler will produce '<filename>.asm' as the output to the same folder. You can then use an assembler to assemble the program. For windows you can use 'nasm' and 'golink'. For the text program you can use the following commands in Windows:
'''
litecc -i test.cl
nasm -f win64 test.asm
golink /entry:main /console kernel32.dll msvcrt.dll test.obj
test.exe
'''

##Interpreter
The interpreter is run with '-i'. The interpreter features:
* Only x64 instructions that the compiler outputs. 
* Full x64 ABI
* Call the built in function print with 'call print' and pass the argument with the first register for function passing
* The interpreter skips everything thru 'SECTION .DATA' and will skip any definition of a built in function

The following instructions are supported:
'''
nop
mov
push
pop
call
ret
add
sub
imul
div
xor
'''