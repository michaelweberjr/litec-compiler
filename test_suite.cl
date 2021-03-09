/* 
	A simple compiler for a small subset of 'C'
	Each function is designed to test the full suite of language options

	The following functions perform tests on the various features supported by the compiler
*/

void testMathOperations()
{
	int x = 13;
	int y = 8;
	int z = x + y;	

	if(z == 21) print(101);
	else print(0);

	if(x - y == 5) print(102);
	else print(0);

	if(x * y == 104) print(103);
	else print(0);

	if(x / 4 == 3) print(104);
	else print(0);

	if(x % 3 == 1) print(105);
	else print(0);

	if(-x == -13) print(106);
	else print(0);

	if(++y == 9) print(107);
	else print(0);

	if(y++ == 9) print(108);
	else print(0);

	if(--y == 9) print(109);
	else print(0);

	if(y-- == 9) print(110);
	else print(0);
}

void testBitOperations()
{
	int x = 255;

	if((x & 15) == 15) print(201);
	else print(0);

	x = 3;
	if((x | 6) == 7) print(202);
	else print(0);

	x = -1;
	if(~x == 0) print(203);
	else print(0);

	x = 1;
	if(x << 4 == 16) print(204);
	else print(0);

	x = 64;
	if(x >> 4 == 4) print(205);
	else print(0);
}

void testOpEqOperations()
{
	int x = 0;
	
	x += 64;
	if(x == 64) print(301);
	else print(0);

	x -= 32;
	if(x == 32) print(302);
	else print(0);

	x *= 2;
	if(x == 64) print(303);
	else print(0);

	x /= 8;
	if(x == 8) print(304);
	else print(0);

	x %= 3;
	if(x == 2) print(305);
	else print(0);

	x |= 255;
	if(x == 255) print(306);
	else print(0);

	x &= 127;
	if(x == 127) print(307);
	else print(0);

	x = -1;
	x ^= 87452169;
	if(x == -87452170) print(308);
	else print(0);

	x = 1;
	x <<= 4;
	if(x == 16) print(309);
	else print(0);

	x = 64;
	x >>= 4;
	if(x == 4) print(310);
	else print(0);
}

void testCmpAndBoolOperations()
{
	int x = 13;
	int y = 5;
	
	if(x == 13) print(401);
	else print(0);

	if(x != y) print(402);
	else print(0);

	if(x > y) print(403);
	else print(0);

	if(x >= y) print(404);
	else print(0);

	if(y < x) print(405);
	else print(0);

	if(y <= x) print(406);
	else print(0);

	if(x >= 13) print(407);
	else print(0);

	if(y <= 8) print(408);
	else print(0);

	if(!x == 0) print(409);
	else print(0);

	x = 0;
	if(!x) print(410);
	else print(0);

	if(y || x) print(411);
	else print(0);

	if(x || y) print(412);
	else print(0);

	if(!(x && y)) print(413);
	else print(0);

	if(!(y && x)) print(414);
	else print(0);

	x = 1;
	if(x && y) print(415);
	else print(0);

	x = 0;
	if((x || y) && !(x && y)) print(416);
	else print(0);
}

int factorial(int x)
{
	if(x <= 1) return 1;
	else return factorial(x-1)*x;
}

void testRecursion()
{
	if(factorial(6) == 720) print(501);
	else print(0);

	if(factorial(-1) == 1) print(502);
	else print(0);
}

void testFlowControl()
{
	int c = 0;
	while(c++ < 4)
	{
		if(c == 1)
		{
			print(601);
		}
		else if(c == 2)
		{
			print(602);
		}
		else if(c == 3)
		{
			print(603);
		}
		else
		{
			print(604);
		}
	}

	/*for(int i = 605; i < 615; i++)
	{
		print(i);
		if(i == 610) break;
		print(0);
	}*/
	
	c = 0;
	while(1)
	{
		if(c++ < 20) continue; 
		print(611);

		while(0)
		{
			print(0);
		}

		while(1)
		{
			if(c++ < 25) continue;
			print(612);
			break;
			print(0);
		}
		print(613);
		break;
		print(0);
	}
	print(614);
}

void main()
{
	testMathOperations();
	testBitOperations();
	testOpEqOperations();
	testCmpAndBoolOperations();
	testRecursion();
	testFlowControl();
	print(1);
}