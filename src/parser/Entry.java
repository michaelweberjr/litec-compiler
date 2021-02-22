package parser;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class Entry {

	public static void main(String[] args) throws Exception {
		ParserError.fileName = "text.cl";
		String text = Files.readString(Path.of("test.cl"));
		runParser(text);	
	}

	public static void runParser(String text) throws Exception
	{
		Tokenizer tokenizer = new Tokenizer(text);
		Parser parser = new Parser(tokenizer.tokens);
		Optimizer.optimize(parser.program);
		FileWriter file = new FileWriter("test.asm");
		CodeGenerator.generate(file, parser.program);
		file.close();
	}
}
