package parser.defs;

public class Symbol {
	public String name;
	public boolean isVar;
	public boolean isFn;
	
	public Symbol(String name)
	{
		this.name = name;
	}
	
	public void setFn()
	{
		isFn = true;
		isVar = false;
	}
	
	public void setVar()
	{
		isFn = false;
		isVar = true;
	}
}
