/* 
	A simple compiler for a small subset of 'C'
	See below for examples of what is supported
*/

// Single line comments are also good
int square(int x)
{
	return x*x;
}

int weirdFunction(int a, int b)
{
	int c = (2 + a) - (3 * b);
	return c + a;
}

void main()
{
	int x = 3 + 4;
	x = x / 2;
	print(weirdFunction(x, square(x - 1)));
}