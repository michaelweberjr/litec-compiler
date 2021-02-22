package parser;

public class ParserError extends Exception {
	private static final long serialVersionUID = 1L;
	public static int lineNum = 1;
	public static String fileName = "";
	
	public ParserError(String errorMsg) {
		super("[ERROR][" + fileName + ":" + lineNum + "] " + errorMsg);
	}
}
