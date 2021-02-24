package parser;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import interpreter.VirtualMachine;

public class Entry {

	public static void main(String[] args) throws Exception {
		boolean compile = true, interpret = true;
		int memory = 65536;
		String input = null;
		String output = null;
		
		for(int i = 0; i < args.length; i++) {
			switch(args[i]) {
			case "-c":
				interpret = false;
				break;
			case "-i":
				compile = false;
				break;
			case "-h":
				printHelp();
				break;
			case "-o":
				output = args[++i];
				break;
			case "-stack":
			case "-s":
				memory = Integer.parseInt(args[++i]);
				break;
			default:
				input = args[i];
				break;
			}
		}
		
		if(input == null) throw new Exception("Error: missing input file");
		if(output == null) {
			if(interpret) output = "temp.asm";
			else {
				int dot = input.indexOf('.');
				output = input.substring(0, dot) + ".asm";
			}
		}
		
		Path inputFile = Path.of(input);
		ParserError.fileName = inputFile.getFileName().toString();
		String text = Files.readString(inputFile);
		if(compile) runParser(text, output);
		
		Path outputFile;
		if(compile) outputFile = Path.of(output);
		else outputFile = Path.of(input);
		text = Files.readString(outputFile);
		
		if(interpret) {
			runVM(text, memory);
			if(outputFile.getFileName().toString().equals("temp.asm")) {
				File toRemove = new File(outputFile.getFileName().toString());
				toRemove.delete();
			}
		}
	}

	public static void runParser(String text, String output) throws Exception {
		Tokenizer tokenizer = new Tokenizer(text);
		Parser parser = new Parser(tokenizer.tokens);
		Optimizer.optimize(parser.program);
		FileWriter file = new FileWriter(output);
		CodeGenerator.generate(file, parser.program);
		file.close();
	}
	
	public static void runVM(String text, int memorySize) throws Exception {
		VirtualMachine vm = new VirtualMachine(text, memorySize);
		vm.run();
	}
	
	private static void printHelp() {
		System.out.println("litec compiler/interpreter - GNU v3.0");
		System.out.println("Usage: litecc [options] inputfile");
		System.out.println("[options]:");
		System.out.println("\t-h\tprint this message");
		System.out.println("\t-c\tcompile only");
		System.out.println("\t-i\tinterpret only");
		System.out.println("\t-h\tprint this message");
		System.out.println("\t-o <name>\tsets the output for compiling to <name>");
		System.out.println("\t-stack\tsets the stack size in bytes [default: 65536]");
		System.out.println("\t-s\tsame as \'-stack\'");
	}
}
