package parser;

import java.nio.file.Files;
import java.nio.file.Path;

public class Entry {

	public static void main(String[] args) throws Exception {
		String text = Files.readString(Path.of("test.cl"));
		runParser(text);	
	}

	public static void runParser(String text) throws Exception
	{
		Tokenizer tokenizer = new Tokenizer(text);
		Parser parser = new Parser(tokenizer.tokens);
		Optimizer.optimize(parser.program);
		parser.printParserTree();
	}
}
