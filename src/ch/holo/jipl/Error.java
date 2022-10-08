package ch.holo.jipl;

import java.util.ArrayList;

public abstract class Error {
	
	public static class IllegalCharError extends Error {
		
		public IllegalCharError(String text, Sequence seq) {
			super("Illegal Character Error", text, seq);
		}

		public void call(String... args) {
			System.err.println(name + " : Illegal character '" + text + "'" + (seq!=null?" at " + seq.toString():""));
		}
		
	}
	
	public static class ExpectedCharError extends Error {

		public ExpectedCharError(String text, Sequence seq) {
			super("Expected Character Error", text, seq);
		}

		public void call(String... args) {
			System.err.println(name + " : Expected character '" + text + "'" + (seq!=null?" at " + seq.toString():""));
		}
		
	}
	
	public static class SyntaxError extends Error {
		
		public SyntaxError(String text, Sequence seq) {
			super("Syntax Error", text, seq);
		}

		public void call(String... args) {
			System.err.println(name + " : " + getText() + (seq!=null?" at " + seq.toString():""));
		}
		
	}
	
	public static class RuntimeError extends Error {

		public RuntimeError(String text, Sequence seq) {
			super("Runtime Error", text, seq);
		}
		
		public RuntimeError(String name, String text, Sequence seq) {
			super(name, text, seq);
		}

		public void call(String... args) {
			System.err.println(name + " : " + text + (seq!=null?" at " + seq.toString():""));
			for(int i = 0; i < trace.size(); i++) {
				Token t = trace.get(i);
				System.err.println("     "+t.value +" at "+t.seq+"");
			}
		}
		
	}
	
	public static class NullPointerError extends RuntimeError {

		public NullPointerError(String text, Sequence seq) {
			super("Null Pointer", text, seq);
		}
		
	}
	
	public static class IllegalArgumentError extends RuntimeError {

		public IllegalArgumentError(String functionName, Object illegalArg, Sequence seq) {
			super("Illegal Argument", illegalArg+" is not allowed in the function '"+functionName+"'", seq);
		}

	}
	
	public static class FileNotFoundError extends RuntimeError {

		public FileNotFoundError(String text, Sequence seq) {
			super("File Not Found", text, seq);
		}
		
	}
	
	public static class Stop extends Error {

		public Stop(String text, Sequence seq) {
			super("Stop", text, seq);
		}

		public void call(String... args) {}
		
	}
	
	protected String name, text;
	protected Sequence seq;
	protected ArrayList<Token> trace;
	
	public Error(String name, String text, Sequence seq) {
		this.name = name;
		this.text = text;
		this.seq = seq;
		this.trace = new ArrayList<>();
		
		if(JIPL.debug) System.err.println("> "+text);
	}
	
	public String getName() { return name; }
	public String getText() { return text; }
	public Sequence getSeq() { return seq; }
	public Error add(Token t) { trace.add(t); return this; }
	
	public abstract void call(String... args);
}
