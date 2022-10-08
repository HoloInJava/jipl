package ch.holo.jipl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import ch.holo.jipl.Interpreter.BuildInObjectClass;
import ch.holo.jipl.Interpreter.Number;
import ch.holo.jipl.Interpreter.RTResult;
import ch.holo.jipl.Interpreter.Value;
import ch.holo.jipl.Parser.ParseResult;

public class JIPL {
	
	public static final boolean debug = false, performance = false;
	public static boolean stop = false;
	
	public static void stop() { stop = true; }
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		Context context = JIPL.getGlobalContext();
		
		if(args.length > 0) {
			for(String f:args)
				run(new File(f), JIPL.getGlobalContext());
			
			scanner.close();
		} else {
			while(true) {
				System.out.print("> ");
				String line = scanner.nextLine();
				if(line.equals("exit"))
					break;
				runAnonymously(line, context);
			}
			scanner.close();
		}
	}
	
	public static Context run(File file, Context context) {
		try {
			ArrayList<String> lines = new ArrayList<String>();
			BufferedReader br = new BufferedReader(new FileReader(file));
			while(true) {
				String s = br.readLine();
				if(s == null) break;
				lines.add(s);
			}
			br.close();
			return run(lines.toArray(new String[lines.size()]), context, file.getAbsolutePath());
		} catch (IOException e) { e.printStackTrace(); }
		return null;
	}
	
	public static Context run(ArrayList<String> lines, Context context, String file) {
		return run(lines.toArray(new String[lines.size()]), context, file);
	}
	
	public static Context run(String[] lines, Context context, String file) {
		return run(String.join("\n", lines), context, file);
	}
	
	public static Context run(String lines, Context context, String file) {
		stop = false;
		if(lines.isEmpty()) return context;
		
		if(debug) System.out.println("Running " + lines);
		
		ArrayList<Token> tokens = Lexer.getTokens(lines, file);
		if(debug) for(Token t:tokens) System.out.println("Lexer: "+t);
		
		ParseResult pr = (ParseResult) new Parser(tokens).parse();
		
		if(pr.error != null) {
			pr.error.call();
			return context;
		}
		Interpreter in = new Interpreter();
		context.file = file;
		
		long m1 = System.currentTimeMillis();
		
		RTResult output = (RTResult) in.visit(pr.node, context);
		if(output.error != null) {
			output.error.call();
			return context;
		}
		
		if(debug) System.out.println("Ended "+output);
		
		if(performance) System.out.println(System.currentTimeMillis()-m1 + " millis.");
		
		return context;
	}
	
	public static ParseResult getParseResult(String lines, String file) {
		stop = false;
		if(lines.isEmpty()) return new ParseResult();
		
		if(debug) System.out.println("Running " + lines);
		
		ArrayList<Token> tokens = Lexer.getTokens(lines, file);
		if(debug) for(Token t:tokens) System.out.println("Lexer: "+t);
		
		ParseResult pr = (ParseResult) new Parser(tokens).parse();
		return pr;
	}
	
	public static Context runAnonymously(String lines, Context context) {
		stop = false;
		if(lines.isEmpty()) return context;
		
		if(debug) System.out.println("Running " + lines);
		
		ArrayList<Token> tokens = Lexer.getTokens(lines, null);
		if(debug) for(Token t:tokens) System.out.println("Lexer: "+t);
		
		ParseResult pr = (ParseResult) new Parser(tokens).parse();
		
		if(pr.error != null) {
			pr.error.call();
			return context;
		}
		
		Interpreter in = new Interpreter();
		context.file = "";
		
		RTResult output = (RTResult) in.visit(pr.node, context);
		if(output.error != null) output.error.call();
		
		return context;
	}
	
	public static final Context getGlobalContext() {
		Context con = new Context("<Global>", null);
		
		con.set("PI", new Number(3.1415927f));
		con.set("PHI", new Number(1.618034f));
		
		JIPLModule.BUILT_IN_FUNCTIONS.generate(con);
		JIPLModule.MATHS_FUNCTIONS.generate(con);
		JIPLModule.IO_FUNCTIONS.generate(con);
		JIPLModule.SCANNER_FUNCTIONS.generate(con);
		
		con.set("Object", new BuildInObjectClass("Object") {
			private static final long serialVersionUID = 1L;
			protected Object generateObject(Context context, RTResult res, Value... args) {
				return res.success(Number.NULL);
			}
		});
		
		return con;
	}
		
}