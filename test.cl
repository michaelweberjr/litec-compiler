int square(int x)
{
	return x*x;
}

void main()
{
	int y = 2;
	int z = square(2+3);
	z = z + 1;
	print(y + 2 * z);
}
