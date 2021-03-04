/* 
	A simple compiler for a small subset of 'C'
	See below for examples of what is supported
*/

// Single line comments are also good
int square(int x)
{
	return x*x;
}

int nestedFunction(int a, int b)
{
	int c = square(7 & 3);
	print(c >= a++);
	return c + a;
}

void main()
{
	int x = 3 + 4;
	x = x / 2;
	print(nestedFunction(x, square(x - 1)));
	++x;
	print(x);
}