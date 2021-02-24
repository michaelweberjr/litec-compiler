package parser;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import interpreter.VirtualMachine;

public class Entry {

	public static void main(String[] args) throws Exception {
		ParserError.fileName = "text.cl";
		String text = Files.readString(Path.of("test.cl"));
		runParser(text);
		text = Files.readString(Path.of("test.asm"));
		runVM(text, 65536);
	}

	public static void runParser(String text) throws Exception {
		Tokenizer tokenizer = new Tokenizer(text);
		Parser parser = new Parser(tokenizer.tokens);
		Optimizer.optimize(parser.program);
		FileWriter file = new FileWriter("test.asm");
		CodeGenerator.generate(file, parser.program);
		file.close();
	}
	
	public static void runVM(String text, int memorySize) throws Exception {
		VirtualMachine vm = new VirtualMachine(text, memorySize);
		vm.run();
	}
}
