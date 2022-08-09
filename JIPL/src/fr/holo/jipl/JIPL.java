package fr.holo.jipl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import fr.holo.jipl.JIPL.Error.IllegalArgumentError;
import fr.holo.jipl.JIPL.Error.RuntimeError;
import fr.holo.jipl.JIPL.Error.SyntaxError;
import fr.holo.jipl.JIPL.Interpreter.BuildInFunction;
import fr.holo.jipl.JIPL.Interpreter.BuildInObjectClass;
import fr.holo.jipl.JIPL.Interpreter.Function;
import fr.holo.jipl.JIPL.Interpreter.List;
import fr.holo.jipl.JIPL.Interpreter.Number;
import fr.holo.jipl.JIPL.Interpreter.ObjectValue;
import fr.holo.jipl.JIPL.Interpreter.RTResult;
import fr.holo.jipl.JIPL.Interpreter.StringValue;
import fr.holo.jipl.JIPL.Interpreter.Value;
import fr.holo.jipl.JIPL.Parser.BinaryOperation;
import fr.holo.jipl.JIPL.Parser.BreakNode;
import fr.holo.jipl.JIPL.Parser.CallNode;
import fr.holo.jipl.JIPL.Parser.CaseDataNode;
import fr.holo.jipl.JIPL.Parser.ContinueNode;
import fr.holo.jipl.JIPL.Parser.ForInNode;
import fr.holo.jipl.JIPL.Parser.ForNode;
import fr.holo.jipl.JIPL.Parser.FunctionDefNode;
import fr.holo.jipl.JIPL.Parser.IfNode;
import fr.holo.jipl.JIPL.Parser.IncludeNode;
import fr.holo.jipl.JIPL.Parser.InstantiateNode;
import fr.holo.jipl.JIPL.Parser.ListNode;
import fr.holo.jipl.JIPL.Parser.NumberNode;
import fr.holo.jipl.JIPL.Parser.ObjectDefNode;
import fr.holo.jipl.JIPL.Parser.ParseResult;
import fr.holo.jipl.JIPL.Parser.PointAccessNode;
import fr.holo.jipl.JIPL.Parser.ReturnNode;
import fr.holo.jipl.JIPL.Parser.StatementsNode;
import fr.holo.jipl.JIPL.Parser.StringNode;
import fr.holo.jipl.JIPL.Parser.ThisNode;
import fr.holo.jipl.JIPL.Parser.UnaryOperation;
import fr.holo.jipl.JIPL.Parser.VarAccessNode;
import fr.holo.jipl.JIPL.Parser.VarAddNode;
import fr.holo.jipl.JIPL.Parser.VarAssignNode;
import fr.holo.jipl.JIPL.Parser.VarModifyNode;
import fr.holo.jipl.JIPL.Parser.VarSubNode;
import fr.holo.jipl.JIPL.Parser.WhileNode;

public class JIPL {
	
	private static final boolean debug = true, performance = false;
	private static boolean stop = false;
	
	public static void stop() { stop = true; }
	
	private static Scanner globalScanner;
	public static void main(String[] args, int u) {
		Context context = JIPL.getGlobalContext();
		
		if(args.length > 0) {
			globalScanner = new Scanner(System.in);
			
			for(String f:args)
				run(new File(f), JIPL.getGlobalContext());
			
			globalScanner.close();
		} else {
			globalScanner = new Scanner(System.in);
			while(true) {
				System.out.print("> ");
				String line = globalScanner.nextLine();
				if(line.equals("exit"))
					break;
				runAnonymously(line, context);
			}
			globalScanner.close();
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
	
	public static Context run(ArrayList<String> lines, Context context, String file) { return run(lines.toArray(new String[lines.size()]), context, file); }
	
	public static Context run(String[] lines, Context context, String file) { return run(String.join("\n", lines), context, file); }
	
	private static boolean tokenError = false;
	public static Context run(String lines, Context context, String file) {
		stop = false;
		tokenError = false;
		if(lines.isEmpty()) return context;
		
		if(debug) System.out.println("Running " + lines);
		
		ArrayList<Token> tokens = Lexer.getTokens(lines, file);
		if(debug) for(Token t:tokens) System.out.println("Lexer: "+t);
		if(tokenError) return context;
		
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
		tokenError = false;
		if(lines.isEmpty()) return new ParseResult();
		
		if(debug) System.out.println("Running " + lines);
		
		ArrayList<Token> tokens = Lexer.getTokens(lines, file);
		if(debug) for(Token t:tokens) System.out.println("Lexer: "+t);
		if(tokenError) return null;
		
		ParseResult pr = (ParseResult) new Parser(tokens).parse();
		
		return pr;
	}
	
	public static Context runAnonymously(String lines, Context context) {
		stop = false;
		tokenError = false;
		if(lines.isEmpty()) return context;
		
		if(debug) System.out.println("Running " + lines);
		
		ArrayList<Token> tokens = Lexer.getTokens(lines, null);
		if(debug) for(Token t:tokens) System.out.println("Lexer: "+t);
		if(tokenError) return context;
		
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
		Context st = new Context("<Global>", null);
		
		st.set("PI", new Number(3.1415927f));
		st.set("PHI", new Number(1.618034f));
		
		addBuildInFunctions(st);
		addMathFunctions(st);
		addIOFunctions(st);
		addScannerFunctions(st);
		addNetworkFunctions(st);
		
		st.set("Object", new BuildInObjectClass("Object") {
			private static final long serialVersionUID = 1L;
			protected Object generateObject(Context context, RTResult res, Value... args) {
				return res.success(Number.NULL);
			}
		});
		
		return st;
	}
	
	private static void addBuildInFunctions(Context st) {
		st.set("print", new BuildInFunction("print", "text") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				System.out.println(""+args[0]);
				return res.success(Number.NULL);
			}
		});
		
		st.set("jipl", new BuildInFunction("jipl", "code") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				Value val = (Value) context.get("code");
				if(val instanceof StringValue) {
					String code = context.string("code");
					JIPL.run(code, context, context.file);
				} else if(val instanceof List) {
					ArrayList<Object> codeObj = context.list("code");
					ArrayList<String> code = new ArrayList<>();
					for(Object o:codeObj)
						code.add(o.toString());
					JIPL.run(code, context, context.file);
				}
				
				return res.success(Number.NULL);
			}
		});
		
		st.set("wait", new BuildInFunction("wait", "value") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number) {
					try { Thread.sleep((long) ((Number)args[0]).value);
					} catch (InterruptedException e) { e.printStackTrace(); }
				} else return res.failure(new Error.RuntimeError("Invalid argument type, "+args[0]+" is not allowed to the function '"+name+"'", seq));
				return res.success(Number.NULL);
			}
		});
		
//		st.set("include", new BuildInFunction("include", "file") {
//			private static final long serialVersionUID = 1L;
//			protected Object executeFunction(Context context, RTResult res, Value... args) {
//				File f = new File(((StringValue) args[0]).value);
//				Context c = JIPL.run(f, context.parent);
//				context.parent.symbolTable.symbols.putAll(c.symbolTable.symbols);
//				return res.success(args[0]);
//			}
//		});
		st.set("d_alloc", new BuildInFunction("d_alloc", "var") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				context.parent.remove(args[0].toString());
				return res.success(Number.NULL);
			}
		});
		st.set("async", new BuildInFunction("async", "fun") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
//				context.parent.symbolTable.remove(args[0].toString());
				if(args[0] instanceof Function) {
					new Thread(() -> {
						args[0].execute();
					}).start();
					return res.success(Number.NULL);
				} else return res.failure(new Error.RuntimeError("Invalid argument type, "+args[0]+" is not allowed to the function '"+name+"'", seq));
			}
		});
	}

	private static void addMathFunctions(Context st) {
		st.set("sin", new BuildInFunction("sin", "value") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number) return res.success(new Number((float)Math.sin(((Number) args[0]).value)));
				return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
			}
		});
		st.set("cos", new BuildInFunction("cos", "value") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number) return res.success(new Number((float)Math.cos(((Number) args[0]).value)));
				return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
			}
		});
		st.set("min", new BuildInFunction("min", "a", "b") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number && args[1] instanceof Number)
					return res.success(new Number(Math.min(((Number)args[0]).value, ((Number)args[1]).value)));
				
				if(!(args[0] instanceof Number))
					return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
				
				return res.failure(new IllegalArgumentError(name, args[1], args[1].seq));
			}
		});
		st.set("max", new BuildInFunction("max", "a", "b") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number && args[1] instanceof Number)
					return res.success(new Number(Math.max(((Number)args[0]).value, ((Number)args[1]).value)));
				
				if(!(args[0] instanceof Number))
					return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
				
				return res.failure(new IllegalArgumentError(name, args[1], args[1].seq));
			}
		});
		st.set("clamp", new BuildInFunction("clamp", "value", "min", "max") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				res.register(checkArgumentTypes(res, args, Number.class, Number.class, Number.class));
				if(res.shouldReturn()) return res;
				return res.success(new Number(Math.max(context.number("min"), Math.min(context.number("max"), context.number("value")))));
			}
		});
		st.set("signum", new BuildInFunction("signum", "value") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number) return res.success(new Number((float)Math.signum(((Number) args[0]).value)));
				return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
			}
		});
		st.set("hexToInt", new BuildInFunction("hexToInt", "value") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof StringValue) return res.success(new Number((float)Integer.parseInt(args[0].toString(), 16)));
				return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
			}
		});
		st.set("abs", new BuildInFunction("abs", "value") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number) return res.success(new Number((float)Math.abs(((Number) args[0]).value)));
				return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
			}
		});
		st.set("floor", new BuildInFunction("floor", "value") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number) return res.success(new Number((float)Math.floor(((Number) args[0]).value)));
				return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
			}
		});
		st.set("ceil", new BuildInFunction("ceil", "value") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number) return res.success(new Number((float)Math.ceil(((Number) args[0]).value)));
				return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
			}
		});
		st.set("toRadians", new BuildInFunction("toRadians", "value") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number) return res.success(new Number((float)Math.toRadians(((Number) args[0]).value)));
				return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
			}
		});
		st.set("toDegrees", new BuildInFunction("toDegrees", "value") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number) return res.success(new Number((float)Math.toDegrees(((Number) args[0]).value)));
				return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
			}
		});
		st.set("random", new BuildInFunction("random") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				return res.success(new Number((float)Math.random()));
			}
		});
		st.set("randomBetween", new BuildInFunction("randomBetween", "min", "max") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				
				float[] values = new float[2];
				for(int i = 0; i < values.length; i++)
					if(args[i] instanceof Number) values[i] = ((Number) args[i]).value;
					else return res.failure(new IllegalArgumentError(name, args[i], args[i].seq));
				
				float bnd = values[1]-values[0];
				if(bnd < 0) return res.success(new Number(0f));
				
				return res.success(new Number(values[0]+new Random().nextInt((int)bnd+1)));
			}
		});
		st.set("sqrt", new BuildInFunction("sqrt", "value") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				if(args[0] instanceof Number) return res.success(new Number((float)Math.sqrt(((Number) args[0]).value)));
				return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
			}
		});
		st.set("distance", new BuildInFunction("distance", "x1", "y1", "x2", "y2") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				float[] values = new float[4];
				for(int i = 0; i < values.length; i++)
					if(args[i] instanceof Number) values[i] = ((Number) args[i]).value;
					else return res.failure(new IllegalArgumentError(name, args[i], args[i].seq));
				return res.success(new Number((float)Math.sqrt((values[0]-values[2])*(values[0]-values[2])+(values[1]-values[3])*(values[1]-values[3]))));
			}
		});
		st.set("modulo", new BuildInFunction("modulo", "value", "diviser") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				float[] values = new float[2];
				for(int i = 0; i < values.length; i++)
					if(args[i] instanceof Number) values[i] = ((Number) args[i]).value;
					else return res.failure(new IllegalArgumentError(name, args[i], args[i].seq));
				return res.success(new Number(values[0]%values[1]));
			}
		});
	}
	
	private static void addIOFunctions(Context st) {
		st.set("File", new BuildInObjectClass("File", "path") {
			private static final long serialVersionUID = 1L;
			protected Object generateObject(Context context, RTResult res, Value... args) {
				
				File f = new File(args[0].toString());
				
				st.set("createNewFile", new BuildInFunction("createNewFile") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						try {
							f.createNewFile();
							return res.success(Number.TRUE);
						} catch (IOException e) { return res.success(Number.FALSE); }
					}
				});
				
				st.set("mkdir", new BuildInFunction("mkdir") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						f.mkdir();
						return res.success(Number.NULL);
					}
				});
				
				st.set("mkdirs", new BuildInFunction("mkdirs") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						f.mkdirs();
						return res.success(Number.NULL);
					}
				});
				
				st.set("readAllLines", new BuildInFunction("readAllLines") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						try {
							BufferedReader br = new BufferedReader(new FileReader(f));
							ArrayList<Object> array = new ArrayList<Object>();
							while(true) {
								String s = br.readLine();
								if(s == null) break;
								array.add(new StringValue(s));
							}
							br.close();
							List l = new List(array);
							return res.success(l);
						} catch (IOException e) { return res.success(Number.FALSE); }
					}
				});
				
				st.set("writeLines", new BuildInFunction("writeLines", "lines") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						try {
							if(args[0] instanceof List) {
								BufferedWriter bw = new BufferedWriter(new FileWriter(f));
								ArrayList<Object> objs = ((List)args[0]).elements;
								
								for(int i = 0; i < objs.size(); i++) {
									Object o = objs.get(i);
									bw.write(o.toString());
									if(i < objs.size()-1) bw.newLine();
								}
								
								bw.flush();
								bw.close();
							} else return res.failure(new Error.RuntimeError("Invalid argument type, "+args[0]+" is not allowed to the function '"+name+"'", seq));

							return res.success(Number.TRUE);
						} catch (IOException e) { return res.success(Number.FALSE); }
					}
				});
				
				return res.success(Number.NULL);
			}
		});
	}
	
	private static void addScannerFunctions(Context st) {
		st.set("readInput", new BuildInFunction("readInput") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				System.out.print("> ");
				return res.success(new StringValue(globalScanner.nextLine()));
			}
		});
	}
	
	private static void addNetworkFunctions(Context st) {
		st.set("openServer", new BuildInFunction("openServer", "port") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				try {
					Network server = new Network();
					Context network = new Context("<network>", context);
					server.open((int) ((Number)args[0]).value, network);
					network.set("port", args[0]);
					network.set("sendToAll", new BuildInFunction("sendToAll", "packet") {
						private static final long serialVersionUID = 1L;
						protected Object executeFunction(Context context, RTResult res, Value... args) {
							server.sendToAll(args[0]);
							return res.success(Number.NULL);
						}
					});
					network.set("close", new BuildInFunction("close") {
						private static final long serialVersionUID = 1L;
						protected Object executeFunction(Context context, RTResult res, Value... args) {
							server.close();
							return res.success(Number.NULL);
						}
					});
					Thread.sleep(250);
					return res.success(new Interpreter.ObjectValue(network));
				} catch (Exception e) { return res.success(Number.FALSE); }
			}
		});
		
		st.set("Packet", new BuildInObjectClass("Packet", "type", "content") {
			private static final long serialVersionUID = 1L;
			protected Object generateObject(Context context, RTResult res, Value... args) {
				return res.success(Number.NULL);
			}
		});
		
		st.set("connectTo", new BuildInFunction("connectTo", "name", "ip", "port") {
			private static final long serialVersionUID = 1L;
			protected Object executeFunction(Context context, RTResult res, Value... args) {
				try {
					Context client = new Context("<client>", context);
					Client c = Client.connect(args[0].toString(), args[1].toString(), (int) ((Number)args[2]).value, client);
					client.set("name", args[0]);
					client.set("ip", args[1]);
					client.set("port", args[2]);
					
					client.set("send", new BuildInFunction("send", "packet") {
						private static final long serialVersionUID = 1L;
						protected Object executeFunction(Context context, RTResult res, Value... args) {
							c.send(args[0]);
							return res.success(args[0]);
						}
					});
					
					c.start();
					Thread.sleep(100);
					return new Interpreter.ObjectValue(client);
				} catch (Exception e) { return res.success(Number.FALSE); }
			}
		});
	}

	public abstract static class Error {
		private static final boolean show = debug;
		
		public static class IllegalCharError extends Error {
			
			public IllegalCharError(String text, Sequence seq) { super("Illegal Character Error", text, seq); if(show) System.err.println("> "+text); }

			public void call(String... args) {
				System.err.println(name + " : Illegal character '" + text + "'" + (seq!=null?" at " + seq.toString():""));
			}
			
		}
		
		public static class ExpectedCharError extends Error {

			public ExpectedCharError(String text, Sequence seq) { super("Expected Character Error", text, seq); if(show) System.err.println("> "+text); }

			public void call(String... args) {
				System.err.println(name + " : Expected character '" + text + "'" + (seq!=null?" at " + seq.toString():""));
			}
			
		}
		
		public static class SyntaxError extends Error {
			
			public SyntaxError(String text, Sequence seq) { super("Syntax Error", text, seq); if(show) System.err.println("> "+text); }

			public void call(String... args) {
				System.err.println(name + " : " + getText() + (seq!=null?" at " + seq.toString():""));
			}
			
		}
		
		public static class RuntimeError extends Error {

			public RuntimeError(String text, Sequence seq) { super("Runtime Error", text, seq); if(show) System.err.println("> "+text); }
			public RuntimeError(String name, String text, Sequence seq) { super(name, text, seq); if(show) System.err.println("> "+text); }

			public void call(String... args) {
				System.err.println(name + " : " + text + (seq!=null?" at " + seq.toString():""));
//				for(int i = trace.size()-1; i >= 0; i--) {
//					Token t = trace.get(i);
//					System.err.println("     "+t.value +" at "+t.seq+"");
//				}
				for(int i = 0; i < trace.size(); i++) {
					Token t = trace.get(i);
					System.err.println("     "+t.value +" at "+t.seq+"");
				}
			}
			
		}
		
		public static class NullPointerError extends RuntimeError {

			public NullPointerError(String text, Sequence seq) { super("Null Pointer", text, seq); if(show) System.err.println("> "+text); }

//			public void call(String... args) {
//				System.err.println(name + " : " + text + (seq!=null?" at " + seq.toString():""));
//			}
			
		}
		
		// TODO DO THIS SHIT
		public static class IllegalArgumentError extends RuntimeError {

			public IllegalArgumentError(String functionName, Object illegalArg, Sequence seq) { super("Illegal Argument", illegalArg+" is not allowed in the function '"+functionName+"'", seq); if(show) System.err.println("> "+text); }

//			public void call(String... args) {
//				System.err.println(name + " : " + text + (seq!=null?" at " + seq.toString():""));
//			}
			
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
		}
		
		public String getName() { return name; }
		public String getText() { return text; }
		public Sequence getSeq() { return seq; }
		public Error add(Token t) { trace.add(t); return this; }
		
		public abstract void call(String... args);
		
	}
	
	public static class Lexer {
		
		public static final String DIGITS = "0123456789";
		public static final String LETTERS = "azertyuiopqsdfghjklmwxcvbnAZERTYUIOPQSDFGHJKLMWXCVBN"+"àèéçôîÀÈÉÇêÊ";
		public static final String LEGAL_CHARS = LETTERS+DIGITS+"_$";
		
		public static final String[] KEYWORDS = {"var", "and", "or", "not", "true", "false", "null", "if", "elseif", "else", "for", "in", "to", "by", "while", "function", "return", "continue", "break", "new", "object", "extends", "this", "include"};
		
		public static ArrayList<Token> getTokens(String text, String file) {
			if(file != null)
				file = new File(file).getName();
			ArrayList<Token> list = new ArrayList<JIPL.Token>();
			char[] chars = text.toCharArray();
			int line = 0;
			for(int i = 0; i < chars.length; i++) {
				char c = chars[i];
				
				if(tokenError) return list;
				
				if(c == ' ') continue;
				else if(c == '\t') continue;
				else if(c == '\n') { line++; list.add(new Token(TokenType.NLINE, new Sequence(line, i, 1, file))); }
				else if(c == ';') list.add(new Token(TokenType.NLINE, new Sequence(line, i, 1, file)));
				else if(c == '(') list.add(new Token(TokenType.LPAREN, new Sequence(line, i, 1, file)));
				else if(c == ')') list.add(new Token(TokenType.RPAREN, new Sequence(line, i, 1, file)));
				else if(c == '[') list.add(new Token(TokenType.LSQUARE, new Sequence(line, i, 1, file)));
				else if(c == ']') list.add(new Token(TokenType.RSQUARE, new Sequence(line, i, 1, file)));
				else if(c == '.') list.add(new Token(TokenType.POINT, new Sequence(line, i, 1, file)));
				else if(c == '{') list.add(new Token(TokenType.LBRA, new Sequence(line, i, 1, file)));
				else if(c == '}') list.add(new Token(TokenType.RBRA, new Sequence(line, i, 1, file)));
				else if(c == '+') i = _unaryComp(chars, i, TokenType.PLUS_EQUAL, TokenType.DPLUS, TokenType.PLUS, line, file, list);
				else if(c == '-') i = _unaryComp(chars, i, TokenType.MINUS_EQUAL, TokenType.DMINUS, TokenType.MINUS, line, file, list);
				else if(c == '*') list.add(new Token(TokenType.MULT, new Sequence(line, i, 1, file)));
				else if(c == '/') list.add(new Token(TokenType.DIV, new Sequence(line, i, 1, file)));
				else if(c == '^') list.add(new Token(TokenType.POWER, new Sequence(line, i, 1, file)));
				else if(c == ':') list.add(new Token(TokenType.COLON, new Sequence(line, i, 1, file)));
				else if(c == ',') list.add(new Token(TokenType.COMMAS, new Sequence(line, i, 1, file)));
				else if(c == '&') list.add(new Token(TokenType.KEYWORD, new Sequence(line, i, 1, file), "and"));
				else if(c == '|') list.add(new Token(TokenType.KEYWORD, new Sequence(line, i, 1, file), "or"));
				else if(c == '!') i = _not_equals(chars, i, line, file, list);
				else if(c == '=') i = _comparator(chars, i, TokenType.DOUBLE_EQUALS, TokenType.EQUALS, line, file, list);
				else if(c == '<') i = _comparator(chars, i, TokenType.LESS_EQUALS, TokenType.LESS, line, file, list);
				else if(c == '>') i = _comparator(chars, i, TokenType.GREATER_EQUALS, TokenType.GREATER, line, file, list);
				else if(c == '"')  i = _string(chars, i, line, '"', file, list);
				else if(c == '\'') i = _string(chars, i, line, '\'', file, list);
				else if(c == '#') i = _comment(chars, i, line, list);
				else if(c == '@') i = _comment(chars, i, line, list);
				else if(DIGITS.contains(c+"")) 	i = _number(chars, i, line, file, list);
				else if(LETTERS.contains(c+"")) i = _identifier(chars, i, line, file, list);
				else {
					new Error.IllegalCharError(c+"", new Sequence(line, i, 1, file)).call();
					tokenError = true;
					return list;
				}
			}
			list.add(new Token(TokenType.END_OF_CODE, new Sequence(line, text.length(), 0, file)));
			return list;
		}
		
		private static int _comment(char[] chars, int i, int line, ArrayList<Token> list) {
			while(true) {
				i++;
				if(i >= chars.length || chars[i] == '\n') break;
			}
			return i;
		}
		
		private static int _number(char[] chars, int index, int line, String file, ArrayList<Token> list) {
			String str = "";
			
			if(chars.length-index > 2 && chars[index] == '0' && chars[index+1] == 'x') {
				for(int i = index+2; i < index+8; i++) {
					if("0123456789abcdef".contains(chars[i]+""))
						str+=chars[i];
					else {
						list.add(new Token(TokenType.INT, new Sequence(line, index, str.length()+2, file), Integer.parseInt(str, 16)+""));
						return i-1;
					}
				}
				list.add(new Token(TokenType.INT, new Sequence(line, index, str.length()+2, file), Integer.parseInt(str, 16)+""));
				return index+7;
			}
			
			TokenType tt = TokenType.INT;
			for(int i = index; i < chars.length; i++) {
				char c = chars[i];
				if(DIGITS.contains(c+"")) str += c;
				else if(c == '.' && tt == TokenType.INT) {
					str += c;
					tt = TokenType.FLOAT;
				} else {
					list.add(new Token(tt, new Sequence(line, index, str.length(), file), str));
					return i-1;
				}
			}
			list.add(new Token(tt, new Sequence(line, index, str.length(), file), str));
			return chars.length;
		}
		
		private static int _string(char[] chars, int index, int line, char begchar, String file, ArrayList<Token> list) {
			String str = "";
			boolean escapingNext = false;
			
			HashMap<Character, String> map = new HashMap<Character, String>();
			map.put('n', "\n");
			map.put('t', "\t");
			map.put('\\', "\\");
			
			for(int i = index+1; i < chars.length; i++) {
				char c = chars[i];
				
				if(escapingNext) {
					str+=map.getOrDefault(c, c+"");
					escapingNext = false;
					continue;
				}
				
				if(c == '\\') {
					escapingNext = true;
					continue;
				}
				
				if(c == begchar || c == '\n') {
					list.add(new Token(TokenType.STRING, new Sequence(line, index, str.length(), file), str));
					return i;
				} else str+=c;
				escapingNext = false;
			}
			
			return chars.length;
		}
		
		private static int _identifier(char[] chars, int index, int line, String file, ArrayList<Token> list) {
			String str = "";
			for(int i = index; i < chars.length; i++) {
				char c = chars[i];
				if(LEGAL_CHARS.contains(c+"")) str += c;
				else break;
			}
			for(String s:KEYWORDS) {
				if(str.equals(s)) {
					list.add(new Token(TokenType.KEYWORD, new Sequence(line, index, str.length(), file), str));
					return index+str.length()-1;
				}
			}
			
			list.add(new Token(TokenType.IDENTIFIER, new Sequence(line, index, str.length(), file), str));
			return index+str.length()-1;
		}
		
		private static int _not_equals(char[] chars, int index, int line, String file, ArrayList<Token> list) {
			if(index < chars.length-1 && chars[index+1] == '=') {
				list.add(new Token(TokenType.NOT_EQUALS, new Sequence(line, index, 2, file)));
				return index+1;
			} else {
				list.add(new Token(TokenType.KEYWORD, new Sequence(line, index, 3, file), "not"));
				return index;
			}
		}
		
		private static int _comparator(char[] chars, int index, TokenType e, TokenType ne, int line, String file, ArrayList<Token> list) {
			if(index < chars.length-1) {
				if(chars[index+1]=='=') {
					list.add(new Token(e, new Sequence(line, index, 2, file)));
					return index+1;
				}
				
				list.add(new Token(ne, new Sequence(line, index, 1, file)));
				return index;
			}
			list.add(new Token(ne, new Sequence(line, index, 1, file)));
			return index;
		}
		
		private static int _unaryComp(char[] chars, int index, TokenType equal, TokenType doub, TokenType solo, int line, String file, ArrayList<Token> list) {
			if(index < chars.length-1) {
				if(chars[index+1]==chars[index]) {
					list.add(new Token(doub, new Sequence(line, index, 2, file)));
					return index+1;
				} else if(chars[index+1]=='=') {
					list.add(new Token(equal, new Sequence(line, index, 2, file)));
					return index+1;
				}
			}
			
			list.add(new Token(solo, new Sequence(line, index, 1, file)));
			return index;
		}
	}
	
	public static class Parser {
		
		public static class NumberNode {
			
			protected Token token;
			public NumberNode(Token token) { this.token = token; }
			
			public String toString() { return token.getValue().toString(); }
			
		}
		
		public static class StringNode {
			protected Token token;
			
			public StringNode(Token token) { this.token = token; }
			public String toString() { return token.toString(); }
		}
		
		public static class BinaryOperation {
			
			protected Object leftNode, rightNode;
			protected Token operationToken;
			
			public BinaryOperation(Object leftNode, Token operationToken, Object rightNode) { 
				this.leftNode = leftNode;
				this.operationToken = operationToken;
				this.rightNode = rightNode;
			}
			
			public String toString() { return "("+leftNode+" "+(operationToken.matches(TokenType.PLUS)?"+":(operationToken.matches(TokenType.MINUS)?"-":operationToken.matches(TokenType.MULT)?"*":operationToken.matches(TokenType.DIV)?"/":"??"))+" "+rightNode+")"; }
			
		}
		
		public static class UnaryOperation {
			
			protected Token operationToken;
			protected Object node;
			
			public UnaryOperation(Token operationToken, Object node) {
				this.operationToken = operationToken;
				this.node = node;
			}
			
			public String toString() { return "(" + operationToken + ", " + node + ")"; }
			
		}
		
		public static class IfNode {
			
			protected ArrayList<CaseDataNode> cases;
			protected CaseDataNode else_case;
			
			public IfNode(ArrayList<CaseDataNode> cases, CaseDataNode else_case) {
				this.cases = cases;
				this.else_case = else_case;
			}
			
			public String toString() { return "if(" + cases + " >> " + else_case + ")"; }
			
		}
		
		public static class VarAssignNode {
			
			protected Token name;
			protected Object expression;
			
			public VarAssignNode(Token name, Object expression) {
				this.name = name;
				this.expression = expression;
			}
			
			public String toString() { return "Assign::"+name+"::"+expression; }
		}
		
		public static class VarAccessNode {
			public Token name;
			public VarAccessNode(Token name) { this.name = name; }
			public String toString() { return /*"Access::"+*/name.getValue().toString(); }
		}
		
		public static class ThisNode {
			public ThisNode() {}
			public String toString() { return "this"; }
		}
		
		public static class VarModifyNode {
			protected Token name;
			protected Object node;
			public VarModifyNode(Token name, Object node) {
				this.name = name;
				this.node = node;
			}
			
			public String toString() { return "Modify{"+name+" = "+node+"}"; }
		}
		
		public static class VarAddNode {
			protected Token name;
			protected Object node;
			
			public VarAddNode(Token name, Object node) {
				this.name = name;
				this.node = node;
			}
			
			public String toString() { return "Modify{"+name+" = "+node+"}"; }
		}
		
		public static class VarSubNode {
			protected Token name;
			protected Object node;
			
			public VarSubNode(Token name, Object node) {
				this.name = name;
				this.node = node;
			}
			
			public String toString() { return "Modify{"+name+" = "+node+"}"; }
		}
		
		public static class ForNode {
			
			protected Token varName;
			protected Object start, end, step, body;
			protected boolean shouldReturnNull;
			
			public ForNode(Token varName, Object startNode, Object endNode, Object stepNode, Object bodyNode, boolean shouldReturnNull) {
				this.varName = varName;
				this.start = startNode;
				this.end = endNode;
				this.step = stepNode;
				this.body = bodyNode;
				this.shouldReturnNull = shouldReturnNull;
			}
			
			public String toString() { return "For"+"::"+body; }
			
		}
		
		public static class ForInNode {
			
			protected Token varName;
			protected Object array, body;
			protected boolean shouldReturnNull;
			
			public ForInNode(Token varName, Object arrayNode, Object bodyNode, boolean shouldReturnNull) {
				this.varName = varName;
				this.array = arrayNode;
				this.body = bodyNode;
				this.shouldReturnNull = shouldReturnNull;
			}
			
			public String toString() { return "ForIn"+"::"+body; }
			
		}
		
		public static class WhileNode {
			
			protected Object condition, body;
			protected boolean shouldReturnNull;
			
			public WhileNode(Object condition, Object bodyNode, boolean shouldReturnNull) {
				this.condition = condition;
				this.body = bodyNode;
				this.shouldReturnNull = shouldReturnNull;
			}
			
			public String toString() { return "While::"+condition+"::"+body; }
			
		}
		
		public static class FunctionDefNode {
			
			protected Token name;
			protected Object body;
			protected Token[] args;
			protected boolean shouldAutoReturn;
			
			public FunctionDefNode(Token name, Object bodyNode, boolean shouldAutoReturn, Token... args) {
				this.name = name;
				this.body = bodyNode;
				this.args = args;
				this.shouldAutoReturn = shouldAutoReturn;
			}
			
			public String toString() { return "Definition of " + name; }
			
		}
		
		public static class CallNode {
			
			protected Token token;
			protected Object nodeToCall;
			protected Object[] args;
			
			public CallNode(Token token, Object nodeToCall, Object... args) {
				this.nodeToCall = nodeToCall;
				this.args = args;
			}
			
			public String toString() { return "Call of "+nodeToCall; }
			
		}
		
		public static class ListNode {
			
			public ArrayList<Object> elementNodes;

			public ListNode(ArrayList<Object> elementNodes) {
				this.elementNodes = elementNodes;
			}
			
			public String toString() {
				return "ListNode>"+elementNodes;
			}
			
		}
		
		public static class StatementsNode {
			
			public ArrayList<Object> elementNodes;

			public StatementsNode(ArrayList<Object> elementNodes) {
				this.elementNodes = elementNodes;
			}
			
			public String toString() {
				return "Statements>"+elementNodes;
			}
			
		}
		
		public static class PointAccessNode {
			
			protected Object[] nodes;
			
			protected PointAccessNode(Object... nodes) {
				this.nodes = nodes;
			}
			
			public String toString() { String str = ""; for(Object o:nodes) str+=o; return "P.A.("+str+")"; }
			
		}
		
		public static class CaseDataNode {
			
			protected Object condition, statements;
			protected boolean shouldReturnNull;
			
			public CaseDataNode(Object condition, Object statements, boolean shouldReturnNull) {
				this.condition = condition;
				this.statements = statements;
				this.shouldReturnNull = shouldReturnNull;
			}
			
			public String toString() { return condition+"><"+statements; }
			
		}
		
		public static class ReturnNode {
			
			protected Object toReturn;
			public ReturnNode(Object toReturn) {
				this.toReturn = toReturn;
			}
			
		}
		
		public static class ContinueNode {}
		public static class BreakNode {}
		
		public static class ObjectDefNode {
			
			protected Token name;
			protected Token[] args;
			protected Object superClass;
			protected Object body;
			
			public ObjectDefNode(Token name, Token[] args, Object body, Object superClass) {
				this.name = name;
				this.args = args;
				this.body = body;
				this.superClass = superClass;
			}
			
			public String toString() { return name+" DEF TO "+args + " TO " + body; }
			
		}
		
		public static class InstantiateNode {
			
			protected Object nodeToCall;
			protected Object[] args;
			
			public InstantiateNode(Object nodeToCall, Object... args) {
				this.nodeToCall = nodeToCall;
				this.args = args;
			}
			
			public String toString() { return "Instantiate of "+nodeToCall; }
			
		}
		
		public static class IncludeNode {
			
			protected Object toInclude;
			public IncludeNode(Object toInclude) {
				this.toInclude = toInclude;
			}
			
		}
		
		public static class ParseResult {
			
			protected Error error = null;
			public Object node = null;
			protected int last_registered_advance = 0, advance_count = 0, reverse_count = 0;
			
			public void register_advancement() {
				last_registered_advance = 1;
				advance_count++;
			}
			
			public Object register(Object node) {
				ParseResult pr = (ParseResult) node;
//				if(pr.error != null)
//					System.out.println("Climbing " + pr.error.text);
				last_registered_advance = pr.advance_count;
				advance_count += pr.advance_count;
				if(pr.error != null) error = pr.error;
				return pr.node;
			}
			
			public Object try_register(ParseResult node) {
				if(node.error != null) {
					reverse_count = node.advance_count;
					if(node.error != null && reverse_count >= 1) error = node.error; //TODO idk
					return node.node;
				}
				return register(node);
			}
			
			public ParseResult success(Object node) {
				this.node = node;
				return this;
			}
			
			public ParseResult failure(Error error) {
				this.error = error;
				return this;
			}
			
			public String toString() {
//				if(error != null) error.call();
				return "PResult("+node+", "+error+")";
			}
		}
		
		protected ArrayList<Token> tokens;
		protected Token currentToken;
		protected int index = -1;
		
		public Parser(ArrayList<Token> tokens) {
			this.tokens = tokens;
			advance();
		}

		private Token advance() {
			index++;
			updateCurrentToken();
			return currentToken;
		}
		
		private Token reverse(int amount) {
			index-=amount;
			if(debug) System.out.println("Parser: Reversing "+amount);
			updateCurrentToken();
			return currentToken;
		}
		
		private void advanceNewLines(ParseResult pr) {
			while(currentToken.matches(TokenType.NLINE)) {
				pr.register_advancement();
				advance();
			}
		}
		
		private void updateCurrentToken() {
			if(index < tokens.size() && index >= 0) currentToken = tokens.get(index);
		}
		
		private Object factor() {
			ParseResult pr = new ParseResult();
			
			advanceNewLines(pr);
			
			Token t = currentToken;
			
			if(t.matches(TokenType.INT, TokenType.FLOAT)) {
				pr.register_advancement();
				advance();
				return pr.success(new NumberNode(t));
			} if(t.matches(TokenType.STRING)) {
				pr.register_advancement();
				advance();
				return pr.success(new StringNode(t));
			} else if(t.matches(TokenType.IDENTIFIER)) {
				pr.register_advancement();
				advance();
				
				if(currentToken.matches(TokenType.EQUALS)) {
					pr.register_advancement();
					advance();
					
					Object o = pr.register(expression());
					if(pr.error != null) return pr;
					
					return pr.success(new VarModifyNode(t, o));
				} else if(currentToken.matches(TokenType.DPLUS)) {
					pr.register_advancement();
					advance();
					return pr.success(new VarAddNode(t, new NumberNode(new Token(TokenType.INT, t.seq, "1"))));
				} else if(currentToken.matches(TokenType.DMINUS)) {
					pr.register_advancement();
					advance();
					return pr.success(new VarSubNode(t, new NumberNode(new Token(TokenType.INT, t.seq, "1"))));
				} else if(currentToken.matches(TokenType.PLUS_EQUAL)) {
					pr.register_advancement();
					advance();
					Object o = pr.register(expression());
					if(pr.error != null) return pr;
					return pr.success(new VarAddNode(t, o));
				} else if(currentToken.matches(TokenType.MINUS_EQUAL)) {
					pr.register_advancement();
					advance();
					Object o = pr.register(expression());
					if(pr.error != null) return pr;
					return pr.success(new VarSubNode(t, o));
				}
				
				return pr.success(new VarAccessNode(t));
			} else if(t.matches(TokenType.LSQUARE)) {
				Object o = pr.register(list_expression());
				if(pr.error != null) return pr;
				return pr.success(o);
			} else if(t.matches(TokenType.PLUS, TokenType.MINUS)) {
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object f = pr.register(point_call());
				if(pr.error != null) return pr;
				return pr.success(new UnaryOperation(t, f));
			} if(t.matches("true", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				return pr.success(new NumberNode(new Token(TokenType.INT, t.seq, "1")));
			} if(t.matches("false", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				return pr.success(new NumberNode(new Token(TokenType.INT, t.seq, "0")));
			} if(t.matches("null", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				return pr.success(new NumberNode(new Token(TokenType.INT, t.seq, "0")));
			} else if(t.matches("if", TokenType.KEYWORD)) {
				Object o = pr.register(if_expression());
				if(pr.error != null) return pr;
				return pr.success(o);
			} else if(t.matches("for", TokenType.KEYWORD)) {
				Object o = pr.register(for_expression());
				if(pr.error != null) return pr;
				return pr.success(o);
			} else if(t.matches("while", TokenType.KEYWORD)) {
				Object o = pr.register(while_expression());
				if(pr.error != null) return pr;
				return pr.success(o);
			} else if(t.matches("function", TokenType.KEYWORD)) {
				Object o = pr.register(function_expression());
				if(pr.error != null) return pr;
				return pr.success(o);
			} else if(t.matches("object", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object o = pr.register(object_expression());
				if(pr.error != null) return pr;
				return pr.success(o);
			} else if(t.matches("new", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object o = pr.register(new_expression());
				if(pr.error != null) return pr;
				return pr.success(o);
			} else if(t.matches("this", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				return pr.success(new ThisNode());
			} else if(t.matches("include", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object expr = pr.register(expression());
				if(pr.error != null) return pr;
				return pr.success(new IncludeNode(expr));
			} else if(t.matches(TokenType.LPAREN)) {
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object ex = pr.register(expression());
				if(pr.error != null) return pr;
				if(currentToken.matches(TokenType.RPAREN)) {
					pr.register_advancement();
					advance();
					advanceNewLines(pr);
					return pr.success(ex);
				} else return pr.failure(new Error.SyntaxError("Expected ')'", currentToken.getSeq()));
			} else if(t.matches(TokenType.LBRA)) {
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object ex = pr.register(statements());
				if(pr.error != null) return pr;
				if(!currentToken.matches(TokenType.RBRA))
					return pr.failure(new Error.SyntaxError("Unexpected <"+t+">", t.getSeq()));
				pr.register_advancement();
				advance();
				return pr.success(ex);
			}
			
			return pr.failure(new Error.SyntaxError("Unexpected <"+t+">", t.getSeq()));
		}
		
		private ParseResult statements() {
			if(debug) System.out.println("Parser: multi-lines statements");
			
			ParseResult pr = new ParseResult();
			ArrayList<Object> statements = new ArrayList<Object>();
			
			advanceNewLines(pr);
			
			if(currentToken.matches(TokenType.RBRA))
				return pr.success(new NumberNode(new Token(TokenType.FLOAT, null, "0")));
			
			Object stat = pr.register(statement());
			if(pr.error != null) return pr;
			statements.add(stat);
			
			boolean more = true;
			while(true) {
				int newline_count = 0;
				
				while(currentToken.matches(TokenType.NLINE)) {
					pr.register_advancement();
					advance();
					newline_count++;
				}
				if(newline_count == 0) more = false;
				
				
				if(!more) break;
				stat = pr.try_register(statement());
				if(pr.error != null)
					return pr;
				if(stat == null) {
					reverse(pr.reverse_count);
					more = false;
					continue;
				}
				
				statements.add(stat);
			}
			
			if(debug) System.out.println("Parser: " + statements);
			
			return pr.success(new StatementsNode(statements));
		}
		
		private ParseResult statement() {
			if(debug) System.out.println("Parser: single statement");
			
			ParseResult pr = new ParseResult();
			
			if(currentToken.matches("return", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object expr = pr.try_register(expression());
				if(expr == null) reverse(pr.reverse_count);
				return pr.success(new ReturnNode(expr==null?Number.NULL:expr));
			} else if(currentToken.matches("continue", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				return pr.success(new ContinueNode());
			} else if(currentToken.matches("break", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				return pr.success(new BreakNode());				
			}
			
			Object expr = pr.register(expression());
			if(pr.error != null)
				return pr;
			
			return pr.success(expr);
		}
		
		private ParseResult list_expression() {
			if(debug) System.out.println("Parser: List node");
			
			ParseResult pr = new ParseResult();
			
			ArrayList<Object> elementNodes = new ArrayList<Object>();
			
			if(!currentToken.matches(TokenType.LSQUARE))
				return pr.failure(new Error.SyntaxError("Expected '['", currentToken.getSeq()));
			
			pr.register_advancement();
			advance();
			
			advanceNewLines(pr);
			
			if(currentToken.matches(TokenType.RSQUARE)) {
				pr.register_advancement();
				advance();
			} else {
				elementNodes.add(pr.register(expression()));
				if(pr.error != null)
					return pr.failure(new Error.SyntaxError("Expected ']'", currentToken.getSeq()));
				while(currentToken.matches(TokenType.COMMAS)) {
					pr.register_advancement();
					advance();
					elementNodes.add(pr.register(expression()));
					if(pr.error != null) return pr;
				}
				advanceNewLines(pr);
				if(!currentToken.matches(TokenType.RSQUARE))
					return pr.failure(new Error.SyntaxError("Expected ']'", currentToken.getSeq()));
				pr.register_advancement();
				advance();
			}
			
			return pr.success(new ListNode(elementNodes));
		}
		
		private ParseResult if_expression() {
			ParseResult pr = new ParseResult();
			IfNode all_cases = new IfNode(new ArrayList<CaseDataNode>(), null);
			pr.register(if_expression_cases("if", all_cases));
			if(pr.error != null) return pr;
			return pr.success(new IfNode(all_cases.cases, all_cases.else_case));
		}
		
		private ParseResult if_expression_cases(String keyword, IfNode node) {
			ParseResult pr = new ParseResult();
			
			if(!currentToken.matches(keyword, TokenType.KEYWORD))
				return pr.failure(new Error.SyntaxError("Expected '"+keyword+"'", currentToken.getSeq()));
			
			pr.register_advancement();
			advance();
			
			advanceNewLines(pr);
			
			Object condition = pr.register(expression());
			if(pr.error != null) return pr;
			
			if(currentToken.matches(TokenType.COLON)) {
//				return pr.failure(new Error.SyntaxError("Expected ':'", currentToken.getSeq()));
				pr.register_advancement();
				advance();
			}
			
			advanceNewLines(pr);
			
			if(currentToken.matches(TokenType.LBRA)) {
				pr.register_advancement();
				advance();
				Object statements = pr.register(statements());
				if(pr.error != null) return pr;
				node.cases.add(new CaseDataNode(condition, statements, true));
				
				if(currentToken.matches(TokenType.RBRA)) {
					pr.register_advancement();
					advance();
				} else {
					return pr.failure(new SyntaxError("Expected '}'", currentToken.getSeq()));
				}
				IfNode tempnode = (IfNode) pr.register(if_expression_bc());
				if(pr.error != null) return pr;
				node.cases.addAll(tempnode.cases);
				node.else_case = tempnode.else_case;
			} else {
				advanceNewLines(pr);
				Object expr = pr.register(statement());
				if(pr.error != null) return pr;
				node.cases.add(new CaseDataNode(condition, expr, false));
				
				IfNode tempnode = (IfNode) pr.register(if_expression_bc());
				if(pr.error != null) return pr;
				node.cases.addAll(tempnode.cases);
				node.else_case = tempnode.else_case;
			}
			
			return pr.success(node);
		}
		
		private Object if_expression_bc() {
			ParseResult pr = new ParseResult();
			IfNode node = new IfNode(new ArrayList<CaseDataNode>(), null);
			
			advanceNewLines(pr);
			
			boolean reverse = true;
			
			if(currentToken.matches("elseif", TokenType.KEYWORD)) {
				Object o = pr.register(if_expression_b());
				if(pr.error != null) return pr;
				IfNode tempnode = (IfNode) o;
				node.cases = tempnode.cases;
				node.else_case = tempnode.else_case;
				reverse = false;
			} else {
				Object o = pr.register(if_expression_c());
				if(pr.error != null) return pr;
				node.else_case = (CaseDataNode) o;
				if(o != null) reverse = false;
			}
			
			if(reverse) reverse(pr.advance_count);
			
			return pr.success(node);
		}

		private Object if_expression_b() {
			return if_expression_cases("elseif", new IfNode(new ArrayList<CaseDataNode>(), null));
		}
		
		private Object if_expression_c() {
			ParseResult pr = new ParseResult();
			Object else_case = null;
			
			if(currentToken.matches("else", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				
				if(currentToken.matches(TokenType.COLON)) {
//					return pr.failure(new Error.SyntaxError("Expected ':'", currentToken.getSeq()));
					pr.register_advancement();
					advance();
				}
				
				advanceNewLines(pr);
				
				if(currentToken.matches(TokenType.LBRA)) {
					pr.register_advancement();
					advance();
					
					Object statements = pr.register(statements());
					if(pr.error != null) return pr;
					
					else_case = new CaseDataNode(null, statements, true);
					
					if(currentToken.matches(TokenType.RBRA)) {
						pr.register_advancement();
						advance();
					} else return pr.failure(new Error.SyntaxError("Expected '}'", currentToken.getSeq()));
				} else {
					advanceNewLines(pr);
					Object expr = pr.register(statement());
					if(pr.error != null) return pr;
					else_case = new CaseDataNode(null, expr, false);
				}
			}
			
			return pr.success(else_case);
		}
		
		private ParseResult for_expression() {
			if(debug) System.out.println("Parser: for");
			
			ParseResult pr = new ParseResult();
			
			if(!currentToken.matches("for", TokenType.KEYWORD))
				return pr.failure(new Error.SyntaxError("Expected 'for'", currentToken.getSeq()));
			
			pr.register_advancement();
			advance();
			
			advanceNewLines(pr);
			
			if(!currentToken.matches(TokenType.IDENTIFIER))
				return pr.failure(new Error.SyntaxError("Expected identifier", currentToken.getSeq()));
			
			Token varName = currentToken;
			
			pr.register_advancement();
			advance();
			
			advanceNewLines(pr);
			
			if(currentToken.matches("in", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				
				Object array = pr.register(expression());
				if(pr.error != null) return pr;
				
				advanceNewLines(pr);
				
				if(currentToken.matches(TokenType.COLON)) {
					pr.register_advancement();
					advance();
					advanceNewLines(pr);
				}
				
				if(currentToken.matches(TokenType.LBRA)) {
					pr.register_advancement();
					advance();

					Object body = pr.register(statements());
					if(pr.error != null) return pr;
					
					if(!currentToken.matches(TokenType.RBRA))
						return pr.failure(new Error.SyntaxError("Expected '}'", currentToken.getSeq()));
					
					pr.register_advancement();
					advance();
					
					return pr.success(new ForInNode(varName, array, body, true));
				}
				
				Object body = pr.register(expression());
				if(pr.error != null) return pr;
				
				return pr.success(new ForInNode(varName, array, body, false));
			}
			
			advanceNewLines(pr);
			
			if(!currentToken.matches(TokenType.EQUALS))
				return pr.failure(new Error.SyntaxError("Expected '='", currentToken.getSeq()));
			
			pr.register_advancement();
			advance();
			
			advanceNewLines(pr);
			
			Object start = pr.register(expression());
			if(pr.error != null) return pr;
			
			advanceNewLines(pr);
			
			if(!currentToken.matches("to", TokenType.KEYWORD))
				return pr.failure(new Error.SyntaxError("Expected 'to'", currentToken.getSeq()));
			
			pr.register_advancement();
			advance();
			
			advanceNewLines(pr);
			
			Object end = pr.register(expression());
			if(pr.error != null) return pr;
			
			Object by = null;
			
			advanceNewLines(pr);
			
			if(currentToken.matches("by", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				by = pr.register(expression());
				if(pr.error != null) return pr;
			}
			
			advanceNewLines(pr);
			
			if(currentToken.matches(TokenType.COLON)) {
//				return pr.failure(new Error.SyntaxError("Expected ':'", currentToken.getSeq()));
				
				pr.register_advancement();
				advance();
			}
			
			advanceNewLines(pr);
			
			if(currentToken.matches(TokenType.LBRA)) {
				pr.register_advancement();
				advance();

				Object body = pr.register(statements());
				if(pr.error != null) return pr;
				
				if(!currentToken.matches(TokenType.RBRA))
					return pr.failure(new Error.SyntaxError("Expected '}'", currentToken.getSeq()));
				
				pr.register_advancement();
				advance();
				
				return pr.success(new ForNode(varName, start, end, by, body, true));
			}
			
			Object body = pr.register(expression());
			if(pr.error != null) return pr;
			
			return pr.success(new ForNode(varName, start, end, by, body, false));
		}
		
		private ParseResult while_expression() {
			if(debug) System.out.println("Parser: while");
			
			ParseResult pr = new ParseResult();
			
			if(!currentToken.matches("while", TokenType.KEYWORD))
				return pr.failure(new Error.SyntaxError("Expected 'while'", currentToken.getSeq()));
			
			pr.register_advancement();
			advance();
			
			advanceNewLines(pr);
			
			Object condition = pr.register(expression());
			if(pr.error != null) return pr;
			
			if(currentToken.matches(TokenType.COLON)) {
				pr.register_advancement();
				advance();
			}
			
			if(currentToken.matches(TokenType.LBRA)) {
				pr.register_advancement();
				advance();
				
				Object body = pr.register(statements());
				if(pr.error != null) return pr;
				
				if(!currentToken.matches(TokenType.RBRA))
					return pr.failure(new Error.SyntaxError("Expected '}'", currentToken.getSeq()));
				
				pr.register_advancement();
				advance();
				
				return pr.success(new WhileNode(condition, body, true));
			}
			
			Object body = pr.register(statement());
			if(pr.error != null) return pr;
			
			return pr.success(new WhileNode(condition, body, false));
		}
		
		private ParseResult function_expression() {
			if(debug) System.out.println("Parser: function");
			
			ParseResult pr = new ParseResult();
			
			if(!currentToken.matches("function", TokenType.KEYWORD))
				return pr.failure(new Error.SyntaxError("Expected 'function'", currentToken.getSeq()));

			pr.register_advancement();
			advance();
			advanceNewLines(pr);
			
			Token function_name = null;
			if(currentToken.matches(TokenType.IDENTIFIER)) {
				function_name = currentToken;
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				if(!currentToken.matches(TokenType.LPAREN))
					return pr.failure(new Error.SyntaxError("Expected '('", currentToken.getSeq()));
			} else {
				advanceNewLines(pr);
				if(!currentToken.matches(TokenType.LPAREN))
					return pr.failure(new Error.SyntaxError("Expected '(' or identifier", currentToken.getSeq()));
			}
			
			pr.register_advancement();
			advance();
			advanceNewLines(pr);
			
			ArrayList<Token> temp_tokens = new ArrayList<JIPL.Token>();
			
			if(currentToken.matches(TokenType.IDENTIFIER)) {
				temp_tokens.add(currentToken);
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				while(currentToken.matches(TokenType.COMMAS)) {
					pr.register_advancement();
					advance();
					advanceNewLines(pr);
					if(!currentToken.matches(TokenType.IDENTIFIER))
						return pr.failure(new Error.SyntaxError("Expected identifier", currentToken.getSeq()));
					temp_tokens.add(currentToken);
					pr.register_advancement();
					advance();
					advanceNewLines(pr);
				}
				if(!currentToken.matches(TokenType.RPAREN))
					return pr.failure(new Error.SyntaxError("Expected ',' or ')'", currentToken.getSeq()));
			} else {
				advanceNewLines(pr);
				if(!currentToken.matches(TokenType.RPAREN))
					return pr.failure(new Error.SyntaxError("Expected identifier or ')'", currentToken.getSeq()));
			}
			
			pr.register_advancement();
			advance();
			advanceNewLines(pr);
			
			if(currentToken.matches(TokenType.COLON)) {
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				
				Object body = pr.register(statement());
				if(pr.error != null) return pr;
				
				return pr.success(new FunctionDefNode(function_name, body, true, temp_tokens.toArray(new Token[temp_tokens.size()])));
			} else {
				if(!currentToken.matches(TokenType.LBRA))
					return pr.failure(new SyntaxError("Expected '{'", currentToken.getSeq()));
				
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				
				Object body = pr.register(statements());
				if(pr.error != null) return pr;
				
				if(!currentToken.matches(TokenType.RBRA))
					return pr.failure(new SyntaxError("Function: expected '}'", currentToken.getSeq()));
				
				pr.register_advancement();
				advance();
				
				return pr.success(new FunctionDefNode(function_name, body, false, temp_tokens.toArray(new Token[temp_tokens.size()])));
			}
			//">"+args.size()
		}
		
		private ParseResult object_expression() {
			if(debug) System.out.println("Parser: object");
			
			ParseResult pr = new ParseResult();
			
			if(!currentToken.matches(TokenType.IDENTIFIER))
				return pr.failure(new Error.SyntaxError("Expected identifier for the object.", currentToken.getSeq()));
			
			Token name = currentToken;
			
			pr.register_advancement();
			advance();
			advanceNewLines(pr);
			
			if(!currentToken.matches(TokenType.LPAREN))
				return pr.failure(new Error.SyntaxError("Expected '('.", currentToken.getSeq()));
			
			pr.register_advancement();
			advance();
			advanceNewLines(pr);
			
			ArrayList<Token> args = new ArrayList<JIPL.Token>();
			if(currentToken.matches(TokenType.IDENTIFIER)) {
				args.add(currentToken);
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				while(currentToken.matches(TokenType.COMMAS)) {
					pr.register_advancement();
					advance();
					advanceNewLines(pr);
					if(!currentToken.matches(TokenType.IDENTIFIER))
						return pr.failure(new Error.SyntaxError("Expected identifier", currentToken.getSeq()));
					args.add(currentToken);
					pr.register_advancement();
					advance();
					advanceNewLines(pr);
				}
			}
			
			if(!currentToken.matches(TokenType.RPAREN))
				return pr.failure(new Error.SyntaxError("Expected ')'.", currentToken.getSeq()));
			
			pr.register_advancement();
			advance();
			
			Object superClass = null;
			if(currentToken.matches("extends", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				if(!currentToken.matches(TokenType.IDENTIFIER))
					return pr.failure(new SyntaxError("Expected identifier", currentToken.getSeq()));
				superClass = new VarAccessNode(currentToken);
				
				pr.register_advancement();
				advance();
			}
			
			if(!currentToken.matches(TokenType.LBRA))
				return pr.failure(new SyntaxError("Expected '{'", currentToken.getSeq()));
			
			pr.register_advancement();
			advance();
			
			Object body = pr.register(statements());
			if(pr.error != null) return pr;
			
			if(!currentToken.matches(TokenType.RBRA))
				return pr.failure(new SyntaxError("Expected '}'", currentToken.getSeq()));
			
			pr.register_advancement();
			advance();
			
			return pr.success(new ObjectDefNode(name, args.toArray(new Token[args.size()]), body, superClass));
		}
		
		private ParseResult new_expression() {
			ParseResult pr = new ParseResult();
			
			if(!currentToken.matches(TokenType.IDENTIFIER))
				return pr.failure(new Error.SyntaxError("Expected identifier", currentToken.getSeq()));
			
			Token name = currentToken;
			
			pr.register_advancement();
			advance();
			advanceNewLines(pr);
			
			if(!currentToken.matches(TokenType.LPAREN))
				return pr.failure(new Error.SyntaxError("Expected '('", currentToken.getSeq()));
			
			pr.register_advancement();
			advance();
			advanceNewLines(pr);
			
			ArrayList<Object> args = new ArrayList<Object>();
			
			if(currentToken.matches(TokenType.RPAREN)) {
				pr.register_advancement();
				advance();
			} else {
				Object expr1 = pr.register(expression());
				if(pr.error != null) return pr;
				args.add(expr1);
				
				while(currentToken.matches(TokenType.COMMAS)) {
					pr.register_advancement();
					advance();
					advanceNewLines(pr);
					args.add(pr.register(expression()));
					if(pr.error != null) return pr;
				}
				
				if(!currentToken.matches(TokenType.RPAREN))
					return pr.failure(new Error.SyntaxError("Expected ')'", currentToken.getSeq()));
				pr.register_advancement();
				advance();
			}
			
			return pr.success(new InstantiateNode(new VarAccessNode(name), args.toArray()));
		}
		
		private ParseResult point_call() {
			ParseResult pr = new ParseResult();
			
			Object atom = pr.register(call());
			if(pr.error != null) return pr;
			
			if(currentToken.matches(TokenType.POINT)) {
				ArrayList<Object> calls = new ArrayList<Object>();
				calls.add(atom);
				
				while(currentToken.matches(TokenType.POINT)) {
					pr.register_advancement();
					advance();
					advanceNewLines(pr);
					if(currentToken.matches(TokenType.LBRA)) {
						pr.register_advancement();
						advance();
						advanceNewLines(pr);
						Object obj = pr.register(statements());
						if(pr.error != null) return pr;
						if(!currentToken.matches(TokenType.RBRA))
							return pr.failure(new SyntaxError("Expected '}'", currentToken.getSeq()));
//						pr.register_advancement();
//						advance();
						advanceNewLines(pr);
						calls.add(obj);
					} else {
						Object obj = pr.register(call());
						if(pr.error != null) return pr;
						calls.add(obj);
					}
				}
				
				return pr.success(new PointAccessNode(calls.toArray()));
			}
			
			return pr.success(atom);
		}
		
		private ParseResult call() {
			ParseResult pr = new ParseResult();
			
			Token associatedToken = currentToken;
			
			Object atom = pr.register(factor());
			if(pr.error != null) return pr;
			
			if(currentToken.matches(TokenType.LPAREN)) {
				if(debug) System.out.println("Parser: call_"+atom);
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				
				ArrayList<Object> args = new ArrayList<Object>();
				
				if(currentToken.matches(TokenType.RPAREN)) {
					pr.register_advancement();
					advance();
				} else {
					args.add(pr.register(expression()));
					if(pr.error != null)
						return pr.failure(new Error.SyntaxError("Expected ')'", currentToken.getSeq()));
					while(currentToken.matches(TokenType.COMMAS)) {
						pr.register_advancement();
						advance();
						advanceNewLines(pr);
						args.add(pr.register(expression()));
						if(pr.error != null) return pr;
					}
					if(!currentToken.matches(TokenType.RPAREN))
						return pr.failure(new Error.SyntaxError("Expected ')'", currentToken.getSeq()));
					pr.register_advancement();
					advance();
				}
				if(atom instanceof VarAccessNode)
					((VarAccessNode) atom).name = new Token(TokenType.IDENTIFIER, ((VarAccessNode) atom).name.getSeq(), ((VarAccessNode) atom).name.value);
				return pr.success(new CallNode(associatedToken, atom, args.toArray(new Object[args.size()])));
			}
			
			return pr.success(atom);
		}
		
		private ParseResult term() {
			ParseResult pr = new ParseResult();
			
			Object left = pr.register(point_call());
			if(pr.error != null) return pr;
			
			while(currentToken.matches(TokenType.MULT, TokenType.DIV)) {
				Token op = currentToken;
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object right = pr.register(point_call());
				if(pr.error != null) return pr;
				left = new BinaryOperation(left, op, right);
			}
			
			return pr.success(left);
		}
		
		private ParseResult comp_expression() {
			ParseResult pr = new ParseResult();
			
			if(currentToken.matches("not", TokenType.KEYWORD)) {
				Token tok = currentToken;
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object o = pr.register(comp_expression());
				if(pr.error != null) return pr;
				
				return pr.success(new UnaryOperation(tok, o));
			}
			
			Object o = pr.register(comp_arith());
			if(pr.error != null) return pr;
			
			return pr.success(o);
		}
		
		private ParseResult comp_binop() {
			ParseResult pr = new ParseResult();
			
			Object left = pr.register(comp_expression());
			if(pr.error != null) return pr;
			
			while(currentToken.matches("and", TokenType.KEYWORD) || currentToken.matches("or", TokenType.KEYWORD)) {
				Token op = currentToken;
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object right = pr.register(comp_expression());
				if(pr.error != null) return pr;
				left = new BinaryOperation(left, op, right);
			}
			
			return pr.success(left);
		}
		
		private ParseResult comp_arith() {
			ParseResult pr = new ParseResult();
			
			Object left = pr.register(arithmetic_expression());
			if(pr.error != null) return pr;
			
			while(currentToken.matches(TokenType.DOUBLE_EQUALS, TokenType.NOT_EQUALS, TokenType.LESS, TokenType.LESS_EQUALS, TokenType.GREATER, TokenType.GREATER_EQUALS)) {
				Token op = currentToken;
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object right = pr.register(arithmetic_expression());
				if(pr.error != null) return pr;
				left = new BinaryOperation(left, op, right);
			}
			
			return pr.success(left);
		}
		
		private ParseResult arithmetic_expression() {
			ParseResult pr = new ParseResult();
			
			Object left = pr.register(term());
			if(pr.error != null) return pr;
			
			while(currentToken.matches(TokenType.PLUS, TokenType.MINUS)) {
				Token op = currentToken;
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				Object right = pr.register(term());
				if(pr.error != null) return pr;
				left = new BinaryOperation(left, op, right);
			}
			
			return pr.success(left);
		}
		
		private ParseResult expression() {
			if(debug) System.out.println("Parser: expression");
			
			ParseResult pr = new ParseResult();
			
			if(currentToken.matches("var", TokenType.KEYWORD)) {
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				
				if(!currentToken.matches(TokenType.IDENTIFIER)) 
					return pr.failure(new Error.SyntaxError("Expected identifier for the variable", currentToken.getSeq()));
				
				Token vname = currentToken;
				
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				
				if(!currentToken.matches(TokenType.EQUALS))
					return pr.failure(new Error.SyntaxError("Expected '='", currentToken.getSeq()));
				
				pr.register_advancement();
				advance();
				advanceNewLines(pr);
				
				Object o = pr.register(expression());
				if(pr.error != null) return pr;
				
				return pr.success(new VarAssignNode(vname, o));
			}
			
			Object o = pr.register(comp_binop());
			if(pr.error != null) return pr;
			
			return pr.success(o);
		}
		
		public Object parse() {
			ParseResult pr = (ParseResult) statements();
			
//			if(pr.error != null && currentToken.getType() == )
			
//			if(pr.error != null)
//				pr.error.call();
//			else
			if(pr.error != null && currentToken.getType() == TokenType.END_OF_CODE)
				return pr;
//			if(pr.error == null && currentToken.getType() != TokenType.END_OF_CODE)
//				return pr.failure(new Error.SyntaxError("Expected end... '+', '-', '*' or '/'", currentToken.getSeq()));
			return pr;
		}
		
	}
	
	public static class Interpreter {
		
		public static class Value implements Serializable {
			private static final long serialVersionUID = 1L;
			protected Context context;
			protected Sequence seq;
			
			protected Error.RuntimeError illegal_operation(Object obj) { return new RuntimeError("Illegal operation with " + obj, seq); }
			
			protected Object execute(Value... args) { System.err.println("No execution defined for " + this); return null; }
			public Value copy() { return this; }
			
			public Context getContext() { return context; }
			
			//TODO: EXPERIMENTAL
			public Value setContext(Context context) {
				if(context == this.context)
					return this;
				if(this.context != null)
					this.context.newParent(context);
				this.context = context;
				return this;
			}
			
			public Context generateContext(Context context) {
				if(context == this.context)
					return this.context;
				if(this.context != null) {
					this.context.newParent(context);
					return this.context;
				}
				Context selfContext = new Context("<value>", context);
//				selfContext.set("this", this);
				this.context = selfContext;
				return this.context;
			}
			
			protected Object add(Object obj) {
				if(context != null && context.symbols.containsKey("add_"))
					return context.function("add_").execute((Value) obj);
				return new StringValue(this+""+obj);
			}
			protected Object sub(Object obj) {
				if(context != null && context.symbols.containsKey("sub_"))
					return context.function("sub_").execute((Value) obj);
				return illegal_operation(obj);
			}
			protected Object mult(Object obj) {
				if(context != null && context.symbols.containsKey("mult_")) {
					return context.function("mult_").execute((Value) obj);
				}
				return illegal_operation(obj);
			}
			protected Object div(Object obj) {
				if(context != null && context.symbols.containsKey("div_"))
					return context.function("div_").execute((Value) obj);
				return illegal_operation(obj);
			}
			
			public Object _equals(Object obj) { return this.equals(obj)?Number.TRUE:Number.FALSE; }
			public Object _not_equals(Object obj) { return _equals(obj)==Number.TRUE?Number.FALSE:Number.TRUE; }
			public Object _less(Object obj) { return illegal_operation(obj); }
			public Object _greater(Object obj) { return illegal_operation(obj); }
			public Object _less_equals(Object obj) { return illegal_operation(obj); }
			public Object _greater_equals(Object obj) { return illegal_operation(obj); }
			public Object _and(Object obj) { return illegal_operation(obj); }
			public Object _or(Object obj) { return illegal_operation(obj); }
			
			public boolean isTrue() { return false; }
			
			public Object _not() { return !isTrue(); }
			
			public Sequence getSeq() { return seq; }
			public Value setSeq(Sequence seq) { this.seq = seq; return this; }
		}
		
		public static class Number extends Value implements Serializable {
			private static final long serialVersionUID = 1L;
			
			public static final Number NULL = new Number(0);
			public static final Number FALSE = new Number(0);
			public static final Number TRUE = new Number(1);
			
			public float value;
			
			public Number(int value) { this.value = value; }
			public Number(float value) { this.value = value; }
			public Number(double value) { this.value = (float)value; }
			
			public Number(Object value) { this.value = Float.parseFloat(value+""); }
			
			protected Object add(Object obj) {
				if(obj instanceof Number) return new Number(value+((Number)obj).value);
				else if(obj instanceof StringValue) return new StringValue(toString()+((StringValue)obj).value);
				else return illegal_operation(obj);
			}
			
			protected Object sub(Object obj) {
				if(obj instanceof Number) return new Number(value-((Number)obj).value); else return illegal_operation(obj);
			}
			
			protected Object mult(Object obj) {
				if(obj instanceof Number) return new Number(value*((Number)obj).value); else return illegal_operation(obj);
			}
			
			protected Object div(Object obj) {
				if(obj instanceof Number) {
					Number n = (Number)obj;
					if(n.value == 0) return new Error.RuntimeError("Division by zero", seq);
					return new Number(value/n.value);
				} else return illegal_operation(obj);
			}
			
			public Object _equals(Object obj) {
				if(obj instanceof Number) return new Number(isEqualTo(((Number)obj).value)?1:0); else return illegal_operation(obj);
			}
			
			public Object _not_equals(Object obj) {
				if(obj instanceof Number) return new Number(!isEqualTo(((Number)obj).value)?1:0); else return illegal_operation(obj);
			}
			
			public Object _less(Object obj) {
				if(obj instanceof Number) return new Number(value<((Number)obj).value?1:0); else return illegal_operation(obj);
			}
			
			public Object _greater(Object obj) {
				if(obj instanceof Number) return new Number(value>((Number)obj).value?1:0); else return illegal_operation(obj);
			}
			
			public Object _less_equals(Object obj) {
				if(obj instanceof Number) return new Number(value<=((Number)obj).value?1:0); else return illegal_operation(obj);
			}
			
			public Object _greater_equals(Object obj) {
				if(obj instanceof Number) return new Number(value>=((Number)obj).value?1:0); else return illegal_operation(obj);
			}
			
			public Object _and(Object obj) {
				if(obj instanceof Number) return new Number((isTrue()&&((Number)obj).isTrue())?1:0); else return illegal_operation(obj);
			}
			
			public Object _or(Object obj) {
				if(obj instanceof Number) return new Number((isTrue()||((Number)obj).isTrue())?1:0); else return illegal_operation(obj);
			}
			
			public boolean isTrue() { return !isEqualTo(0); }
			public Object _not() { return new Number(value==0?1:0); }
			
			public float getValue() { return value; }
			public void setValue(float value) { this.value = value; }
			
			public boolean isEqualTo(float x) { return Math.abs(value-x) < 0.00025f; }
			
			public String toString() { return (value%1==0?(int)value+"":value+""); }
			
			public Value copy() { return new Number(value).setContext(this.context); }
		}
		
		public static class StringValue extends Value implements Serializable {
			private static final long serialVersionUID = 1L;
			
			protected String value;
			
			public StringValue(String value) { this.value = value; }
			public StringValue(Object value) { this.value = (String) value; }
			
			protected Object add(Object obj) { return new StringValue(value+obj.toString()); }
			
			public boolean isTrue() { return value.length() > 0; }
			
			public Context generateContext(Context context) {
//				if(this.context != null)
				//System.out.println("TEST HERE");
				if(this.context != null) {
					this.context.newParent(context);
					return this.context;
				}
				Context selfContext = new Context("<string>", context);
				selfContext.set("length", new Number(value.length()));
				selfContext.set("equals", new BuildInFunction("equals", "string") {
					private static final long serialVersionUID = 1L;

					protected Object executeFunction(Context context, RTResult res, Value... args) {
						return value.equalsIgnoreCase(args[0].toString())?Number.TRUE:Number.FALSE;
					}
				});
				selfContext.set("split", new BuildInFunction("split", "text") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						String s = args[0].toString();
						String[] strs = s.equals(" ")?value.split("\\s+"):value.split(s);
						StringValue[] vls = new StringValue[strs.length];
						for(int i = 0; i < strs.length; i++)
							vls[i] = new StringValue(strs[i]);
						
						return res.success(new List(vls));
					}
				});
				selfContext.set("charAt", new BuildInFunction("charAt", "index") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
//						RTResult res = new RTResult();
						if(args[0] instanceof Number) {
							int index = (int) ((Number)args[0]).value;
							if(index < 0 || index >= value.length())
								return res.failure(new Error.RuntimeError("Index out of bounds " + index + ".", null));
							return res.success(new StringValue(""+value.charAt(index)));
						}
						return res.failure(new Error.RuntimeError("Invalid argument type, "+args[0]+" is not allowed to the function '"+name+"'", seq));
					}
				});
				selfContext.set("chars", new BuildInFunction("chars") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
//						RTResult res = new RTResult();
						StringValue[] chars = new StringValue[value.length()];
						for(int i = 0; i < chars.length; i++)
							chars[i] = new StringValue(value.charAt(i)+"");
						return res.success(new List(chars));
					}
				});
				selfContext.set("substring", new BuildInFunction("sub", "start", "end") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
//						RTResult res = new RTResult();
						
						if(args[0] instanceof Number && args[1] instanceof Number) {
							int start = (int) ((Number)args[0]).value, end = (int) ((Number)args[1]).value;
							if(start < 0 || start > value.length())
								return res.failure(new Error.RuntimeError("Index out of bounds " + start + ".", null));
							if(end < 0 || end > value.length())
								return res.failure(new Error.RuntimeError("Index out of bounds " + end + ".", null));
							return res.success(new StringValue(value.substring(start, end)));
						}
						
						return res.failure(new Error.RuntimeError("Invalid argument type, "+args[0]+"::"+args[1]+" is not allowed to the function '"+name+"'", seq));
					}
				});
//				selfContext.set("this", this);
				this.context = selfContext;
				return selfContext;
			}
			
			public String toString() { return value+""; }
		}
		
		public static class BaseFunction extends Value implements Serializable {
			private static final long serialVersionUID = 1L;
			
			public String name;
			public String[] args_name;
			
			public BaseFunction(String name) {
				this.name = name==null?"<anonymous>":name;
			}
			
			protected Context generateNewContext() {
				Context new_context = new Context(name, context);
				return new_context;
			}
			
			protected Context generateNewContext(Context context) {
				Context new_context = new Context(name, context);
				return new_context;
			}
			
			protected Object check_args(String[] args_name, Value[] args) {
				RTResult res = new RTResult();
				if(args.length != args_name.length)
					return res.failure(new Error.RuntimeError("Incorrect number of argument have been passed in " + name, seq));
				return res.success(null);
			}
			
			protected void populate_args(String[] args_name, Value[] args, Context exec_context) {
				for(int i = 0; i < args.length; i++) {
					String arg_name = args_name[i];
					Value arg_value = args[i];
//					arg_value.setContext(exec_context);
//					if(arg_value.context)
					arg_value.generateContext(exec_context);
					exec_context.set(arg_name, arg_value);
				}
			}
			
			protected Object checkThenPopulate(String[] args_name, Value[] args, Context exec_context) {
				RTResult res = new RTResult();
				res.register(check_args(args_name, args));
				if(res.shouldReturn()) return res;
				populate_args(args_name, args, exec_context);
				return res.success(null);
			}

			public Object checkArgumentTypes(RTResult res, Value[] args, Class<?>... cl) {
				for(int i = 0; i < args.length; i++)
					if(!cl[i].isInstance(args[i])) return res.failure(new IllegalArgumentError(name, args[i], args[i].seq));
				return res.success(Number.NULL);
			}
			
			protected String fn_name() { return name+">"+args_name.length; }
		}
		
		public static abstract class BuildInFunction extends BaseFunction implements Serializable {
			private static final long serialVersionUID = 1L;
			
			public BuildInFunction(String name, String... args_name) {
				super(name);
				this.args_name = args_name;
			}
			
			public Object execute(Value... args) {
				RTResult res = new RTResult();
				Context con = generateNewContext();
				
				res.register(checkThenPopulate(args_name, args, con));
				if(res.shouldReturn()) return res;
				
				Object ret = res.register(executeFunction(con, new RTResult(), args));
				if(res.shouldReturn()) return res;
				
				return res.success(ret);
			}
			
			protected abstract Object executeFunction(Context context, RTResult res, Value... args);
			
			public String toString() {
				return "<"+name+">";
			}
			
		}
		
		public static class Function extends BaseFunction implements Serializable {
			private static final long serialVersionUID = 1L;
			
			protected Object body_node;
			protected boolean shouldAutoReturn;
			
			public Function(String name, Object body_node, String[] args_name, boolean shouldAutoReturn) {
				super(name);
				this.body_node = body_node;
				this.args_name = args_name;
				this.shouldAutoReturn = shouldAutoReturn;
			}
			
			public Object execute(Value... args) {
				RTResult res = new RTResult();
				Interpreter intepreter = new Interpreter();
				Context new_context = generateNewContext();
				
				res.register(checkThenPopulate(args_name, args, new_context));
				if(res.shouldReturn()) return res;
				Object value = res.register(intepreter.visit(body_node, new_context));
				
				if(res.shouldReturn() && res.returnValue == null) return res;
				
				Object ret = shouldAutoReturn?value:(res.returnValue!=null?res.returnValue:Number.NULL);
				return res.success(ret);
			}
			
			public Value copy() {
				Function func = new Function(name, body_node, args_name, shouldAutoReturn);
				func.setContext(context);
				func.setSeq(seq);
				return func;
			}
			
			public String toString() { return "<function "+name+">"; }
			
		}
		
		public static class List extends Value implements Serializable {
			private static final long serialVersionUID = 1L;
			
			protected ArrayList<Object> elements;
			public List(ArrayList<Object> elements) { this.elements = elements; }
			public List(Object[] elements) { this.elements = new ArrayList<Object>(); for(Object o:elements) this.elements.add(o); }
			
			public Value copy() {
				List list = new List(new ArrayList<Object>());
				list.elements.addAll(elements);
				list.context = this.context;
				return list;
			}
			
			public Object add(Object obj) {
				List list = (List) copy();
				if(obj instanceof List) list.elements.addAll(((List) obj).elements);
				else list.elements.add(obj);
				return list;
			}
			
			public Object mult(Object obj) {
				List list = (List) copy();
				list.elements.add(obj);
				return list;
			}
			
			public Context generateContext(Context context) {
				Context selfContext = new Context("<list>", context);
				selfContext.set("add", new BuildInFunction("add", "element") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						elements.add(args[0]);
						return res.success(args[0]);
					}
				});
				selfContext.set("get", new BuildInFunction("get", "index") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						if(args[0] instanceof Number) {
							int index = (int) ((Number)args[0]).value;
							if(index < 0 || index >= elements.size())
								return res.failure(new Error.RuntimeError("Index out of bounds " + index, null));
							return res.success(elements.get(index));
						}
						return res.failure(new Error.RuntimeError("Invalid argument type, "+args[0]+" is not allowed to the function '"+name+"'", seq));
					}
				});
				selfContext.set("set", new BuildInFunction("set", "index", "object") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						if(args[0] instanceof Number) {
							int index = (int) ((Number)args[0]).value;
							if(index < 0 || index >= elements.size())
								return res.failure(new Error.RuntimeError("Index out of bounds " + index, null));
							elements.set(index, args[1]);
							return args[1];
						}
						return res.failure(new Error.RuntimeError("Invalid argument type, "+args[0]+" is not allowed to the function '"+name+"'", seq));
					}
				});
				selfContext.set("insert", new BuildInFunction("insert", "index", "object") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						if(args[0] instanceof Number) {
							int index = (int) ((Number)args[0]).value;
							if(index < 0 || index > elements.size())
								return res.failure(new Error.RuntimeError("Index out of bounds " + index, null));
							elements.add(index, args[1]);
							return args[1];
						}
						return res.failure(new Error.RuntimeError("Invalid argument type, "+args[0]+" is not allowed to the function '"+name+"'", seq));
					}
				});
				selfContext.set("join", new BuildInFunction("join", "by") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						String el = "";
						for(int i = 0; i < elements.size(); i++) {
							el+=elements.get(i);
							if(i!=elements.size()-1)
								el+=args[0];
						}
						return res.success(new StringValue(el));
					}
				});
				selfContext.set("indexOf", new BuildInFunction("indexOf", "obj") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						return res.success(new Number(elements.indexOf(args[0])));
					}
				});
				selfContext.set("clear", new BuildInFunction("clear") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						elements.clear();
						return res.success(Number.NULL);
					}
				});
				selfContext.set("foreach", new BuildInFunction("foreach", "function") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
//						RTResult res = new RTResult();
						
						BaseFunction fun = null;
						if(args[0] instanceof BaseFunction) fun = (BaseFunction) args[0];
						else return res.failure(new RuntimeError("Invalid argument type, "+args[0]+" is not allowed in the function '"+name+"'", null));
						
						List l = new List(new ArrayList<Object>());
						for(Object el:elements) {
							Object o = res.register(fun.execute((Value) el));
							if(res.shouldReturn()) return res;
							l.elements.add(o);
						}
						
						return res.success(l);
					}
				});
				selfContext.set("size", new BuildInFunction("size") {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) { return new Number(elements.size()); }
				});
				
				this.context = selfContext;
				this.context.newParent(context);
				return selfContext;
			}
			
			public ArrayList<Object> getElements() { return elements; }
			public String toString() { return elements.toString(); }
			
		}
		
		public static class ObjectClass extends BaseFunction implements Serializable {
			private static final long serialVersionUID = 1L;
			
			protected Object body, superClass;
			
			public ObjectClass(String name, String[] args_name, Object body, Object superClass) {
				super(name);
				this.args_name = args_name;
				this.body = body;
				this.superClass = superClass;
			}
			
			public Object execute(Context con, Value... args) {
				RTResult res = new RTResult();
				
				Interpreter interpreter = new Interpreter();
				Context new_context = generateNewContext(con);
				
				ObjectValue obj = new ObjectValue(new_context);
//				new_context.set("this", obj);
				new_context.set("type", this);
				
				if(superClass instanceof ObjectClass) {
					new_context.set("super", new BuildInFunction("super", ((ObjectClass) superClass).args_name) {
						private static final long serialVersionUID = 1L;
						protected Object executeFunction(Context context, RTResult res, Value... args) {
//							RTResult res = new RTResult();
							Object extend = res.register(((ObjectClass) superClass).execute(context, args));
							
							if(res.error != null)
								return res;
							
							//System.out.println(">>>"+extend);
							
							if(extend instanceof ObjectValue)
								new_context.symbols.putAll(((ObjectValue) extend).selfContext.symbols);
							
//							new_context.set("this", obj);
							new_context.set("type", getThis());
							
							new_context.set("super", extend);
							
							return res.success(Number.NULL);
						}
					});
				}
				
				
				res.register(checkThenPopulate(args_name, args, new_context));
				if(res.error != null) return res;
				
				res.register(interpreter.visit(body, new_context));
				if(res.error != null) return res;
				
				return res.success(obj);
			}
			
			public String toString() { return "object "+name; }
			
			public ObjectClass getThis() { return this; }
			
		}
		
		public static abstract class BuildInObjectClass extends ObjectClass implements Serializable {
			private static final long serialVersionUID = 1L;

			public BuildInObjectClass(String name, String... args_name) {
				super(name, args_name, null, null);
			}
			
			public Object execute(Context con, Value... args) {
				RTResult res = new RTResult();
				
				Context new_context = generateNewContext(con);
				res.register(checkThenPopulate(args_name, args, new_context));
				if(res.error != null) return res;
				
				res.register(generateObject(new_context, new RTResult(), args));
				if(res.error != null) return res;
				
				ObjectValue obj = new ObjectValue(new_context);
//				new_context.set("this", obj);
				new_context.set("type", this);
				return res.success(obj);
			}
			
			protected abstract Object generateObject(Context context, RTResult res, Value... args);
			
		}
		
		public static class ObjectValue extends Value implements Serializable {
			private static final long serialVersionUID = 1L;
			
			public Context selfContext;
			public ObjectValue(Context selfContext) { this.selfContext = selfContext; }
			
			public Context generateContext(Context context) {
				this.context = context;
//				this.context.newParent(context);
//				if(this.context != null) {
//					return this.selfContext;
//				}
				selfContext.parent = context;
				return selfContext;
			}
			
			protected Object add(Object obj) {
				if(selfContext != null && selfContext.symbols.containsKey("add_"))
					return selfContext.function("add_").execute((Value) obj);
				return new StringValue(this+""+obj);
			}
			protected Object sub(Object obj) {
				if(selfContext != null && selfContext.symbols.containsKey("sub_"))
					return selfContext.function("sub_").execute((Value) obj);
				return illegal_operation(obj);
			}
			protected Object mult(Object obj) {
				if(selfContext != null && selfContext.symbols.containsKey("mult_")) {
					return selfContext.function("mult_").execute((Value) obj);
				}
				return illegal_operation(obj);
			}
			protected Object div(Object obj) {
				if(selfContext != null && selfContext.symbols.containsKey("div_"))
					return selfContext.function("div_").execute((Value) obj);
				return illegal_operation(obj);
			}
			
			public Value copy() { return this; }
			
			public String className() { return ((ObjectClass)selfContext.get("type")).name; }
			
			public String toString() {
//				@SuppressWarnings("unchecked")
//				HashMap<String, Object> symbols = (HashMap<String, Object>) selfContext.symbols.clone();
//				symbols.remove("this");
//				symbols.remove("super");
//				String str = ((ObjectClass)symbols.get("type")!=null?((ObjectClass)symbols.get("type")).name:"")+"(";
//				symbols.remove("type");
//				System.out.println(selfContext);
//				System.out.println(symbols);
//				for(String s:symbols.keySet()) {
//					str+=s + " = "+symbols.get(s)+", ";
//				}
//				if(symbols.size() > 0) str = str.substring(0, str.length()-2)+")";
//				else str+=")";
				return "Object("+selfContext+")";
			}
		}
		
		public static class RTResult {
			
			public Object value = null, returnValue = null;
			protected Error error = null;
			protected boolean shouldContinue = false, shouldBreak = false;
			public Token associatedToken;
			
			public void reset() {
				error = null;
				value = null;
				returnValue = null;
				shouldContinue = false;
				shouldBreak = false;
			}
			
			public Object register(Object value) {
//				System.out.println(associatedToken+", "+value);
//				System.out.println(value + " ?");
				if(value instanceof RTResult) {
					RTResult pr = (RTResult) value;
					if(pr.error != null) {
//						System.out.println("climbing " + pr.error.name + ", "+pr.associatedToken);
						if(associatedToken != null)
							pr.error.add(associatedToken);
						error = pr.error;
						associatedToken = pr.associatedToken;
						return this;
					}
					returnValue = pr.returnValue;
					shouldContinue = pr.shouldContinue;
					shouldBreak = pr.shouldBreak;
					associatedToken = pr.associatedToken; // TODO?
					return pr.value;
				}
				if(value instanceof Error) {
//					System.out.println("test3");
					error = (Error) value;
					return this;
				}
				return value;
			}
			
			public RTResult success(Object value) {
				reset();
				this.value = value;
				return this;
			}
			
			public RTResult success_return(Object value) {
				reset();
				returnValue = value;
				return this;
			}
			
			public RTResult success_continue() {
				reset();
				shouldContinue = true;
				return this;
			}
			
			public RTResult success_break() {
				reset();
				shouldBreak = true;
				return this;
			}
			
			public RTResult failure(Error value) {
				reset();
				this.error = value;
				return this;
			}
			
			public boolean shouldReturn() {
				return error != null || returnValue != null || shouldContinue || shouldBreak;
			}
			
			public String toString() {
//				if(error != null) error.call();
//				else if(value != null) return value.toString();
				return "RTResult("+value+", "+error+")";
			}
		}
		
		public Object visit(Object node, Context context) {
//			if(node.toString().contains("Instantiate"))
//				debug = true;
			if(debug) System.out.println("Intepreter: Visit " + node); //+ "["+context+"]");
			if(stop) return new RTResult().failure(new Error.Stop("Stop.", null));
			
				 if(node instanceof NumberNode) 		return visitNumberNode		((NumberNode) node, 		context);
			else if(node instanceof StringNode) 		return visitStringNode		((StringNode) node, 		context);
			else if(node instanceof BinaryOperation) 	return visitBinaryOperation	((BinaryOperation) node,	context);
			else if(node instanceof UnaryOperation) 	return visitUnaryOperation	((UnaryOperation) node, 	context);
			else if(node instanceof VarAccessNode)		return visitVarAccessNode	((VarAccessNode) node, 		context);
			else if(node instanceof ThisNode)			return visitThisNode		((ThisNode) node, 			context);
			else if(node instanceof VarAssignNode) 		return visitVarAssignNode	((VarAssignNode) node, 		context);
			else if(node instanceof IfNode) 			return visitIfNode			((IfNode) node,				context);
			else if(node instanceof VarModifyNode) 		return visitVarModifyNode	((VarModifyNode) node, 		context);
			else if(node instanceof VarAddNode) 		return visitVarAddNode		((VarAddNode) node, 		context);
			else if(node instanceof VarSubNode) 		return visitVarSubNode		((VarSubNode) node,			context);
			else if(node instanceof ForNode) 			return visitForNode			((ForNode) node,			context);
			else if(node instanceof ForInNode) 			return visitForInNode		((ForInNode) node, 			context);
			else if(node instanceof WhileNode) 			return visitWhileNode		((WhileNode) node, 			context);
			else if(node instanceof CallNode) 			return visitCallNode		((CallNode) node, 			context);
			else if(node instanceof FunctionDefNode) 	return visitFunctionDefNode	((FunctionDefNode) node, 	context);
			else if(node instanceof ListNode)			return visitListNode		((ListNode) node, 			context);
			else if(node instanceof StatementsNode)		return visitStatementsNode	((StatementsNode) node,		context);
			else if(node instanceof ReturnNode)			return visitReturnNode		((ReturnNode) node, 		context);
			else if(node instanceof ContinueNode)		return visitContinueNode	((ContinueNode) node, 		context);
			else if(node instanceof BreakNode)			return visitBreakNode		((BreakNode) node, 			context);
			else if(node instanceof PointAccessNode)	return visitPointAccessNode	((PointAccessNode) node,	context);
			else if(node instanceof InstantiateNode)	return visitInstantiateNode	((InstantiateNode) node,	context);
			else if(node instanceof ObjectDefNode)		return visitObjectDefNode	((ObjectDefNode) node,		context);
			else if(node instanceof IncludeNode)		return visitIncludeNode		((IncludeNode) node, 		context);
			
//			if(debug)
			System.out.println(node);
			System.err.println("Intepreter: No visit for " + node/*.getClass().getSimpleName()*/ + ".");
//			return new RTResult().failure(new RuntimeError("Impossible interpretation.", null));
			return node;
		}

		private Object visitNumberNode(NumberNode node, Context context) {
			return new RTResult().success(new Number(node.token.getValue()).setSeq(node.token.getSeq()));
		}
		
		private Object visitStringNode(StringNode node, Context context) {
			return new RTResult().success(new StringValue(node.token.getValue()).setSeq(node.token.getSeq()));
		}
		
		private Object visitVarAccessNode(VarAccessNode node, Context context) {
			RTResult res = new RTResult();
			String vname = (String) node.name.value;
			Object value = context.get(vname);
			
			res.associatedToken = node.name;
			
			if(value == Number.NULL) {
//				System.out.println(vname+" is null : "+context + ", \n" + context.parent+"\n"+context.parent+"\n"+context.parent);
				return res.failure(new Error.NullPointerError(vname + " is not defined", node.name.getSeq()));
			}
			
			return res.success(value);
		}
		
		private Object visitThisNode(ThisNode node, Context context) {
			RTResult res = new RTResult();
//			String vname = (String) node.name.value;
//			Object value = context.symbolTable.get(vname);
//			
//			if(value == null) return res.failure(new Error.RuntimeError(vname + " is not defined", node.name.getSeq()));
			return res.success(new ObjectValue(context));
		}
		
		private Object visitVarAssignNode(VarAssignNode node, Context context) {
			RTResult res = new RTResult();
			
			String vname = (String) node.name.value;
			Object value = res.register(visit(node.expression, context));
			
			if(res.shouldReturn()) return res;
			
			context.set(vname, value);
			
			return res.success(value);
		}
		
		private Object visitVarModifyNode(VarModifyNode node, Context context) {
			RTResult res = new RTResult();
			
			String name = (String) node.name.value;
			Object value = res.register(visit(node.node, context));
			if(res.shouldReturn()) return res;
			if(!name.equals("this")) {
				if(context.parent == null || context.displayName.equals("<Global>")) context.set(name, value);
				else context.getSource(name).set(name, value);
			}
			
			return res.success(value);
		}
		
		private Object visitVarAddNode(VarAddNode node, Context context) {
			RTResult res = new RTResult();
			
			String name = (String) node.name.value;
			Object value = res.register(visit(node.node, context));
			if(res.shouldReturn()) return res;
			
			if(!name.equals("this")) {
//				SymbolTable st = context.symbolTable.getSourcePrime(name);
				Context con = context.getSource(name);
				con.set(name, ((Value) con.get(name)).add(value));
			}
			
			return res.success(value);
		}
		
		private Object visitVarSubNode(VarSubNode node, Context context) {
			RTResult res = new RTResult();
			
			String name = (String) node.name.value;
			Object value = res.register(visit(node.node, context));
			if(res.shouldReturn()) return res;
			
			if(!name.equals("this")) {
//				SymbolTable st = context.symbolTable.getSourcePrime(name);
				Context con = context.getSource(name);
				con.set(name, ((Value) con.get(name)).sub(value));
			}
			
			return res.success(value);
		}
		
		private Object visitBinaryOperation(BinaryOperation node, Context context) {
			RTResult res = new RTResult();
			
			Object leftObj = res.register(visit(node.leftNode, context));
			if(res.shouldReturn()) return res;
			Value left = (Value) leftObj;
			
			Object rightObj = (Object)res.register(visit(node.rightNode, context));
			if(res.shouldReturn()) return res;
			Value right = (Value) rightObj;
			
				 if(node.operationToken.matches(TokenType.PLUS)) 	return res.register(left.add(right));
			else if(node.operationToken.matches(TokenType.MINUS))	return res.register(left.sub(right));
			else if(node.operationToken.matches(TokenType.MULT)) 	return res.register(left.mult(right));
			else if(node.operationToken.matches(TokenType.DIV)) {
//				if(right instanceof Number)
//					left.seq = node.operationToken.getSeq();
//				Object o = left.div(right);
//				if(o instanceof Error) return res.failure((Error) o);
//				return res.success(o);
				return res.register(left.div(right));
			} else if(node.operationToken.matches(TokenType.DOUBLE_EQUALS))	return res.register(left._equals(right));
			else if(node.operationToken.matches(TokenType.NOT_EQUALS)) 		return res.register(left._not_equals(right));
			else if(node.operationToken.matches(TokenType.LESS)) 			return res.register(left._less(right));
			else if(node.operationToken.matches(TokenType.LESS_EQUALS)) 	return res.register(left._less_equals(right));
			else if(node.operationToken.matches(TokenType.GREATER)) 		return res.register(left._greater(right));
			else if(node.operationToken.matches(TokenType.GREATER_EQUALS)) 	return res.register(left._greater_equals(right));
			else if(node.operationToken.matches("and", TokenType.KEYWORD)) 	return res.register(left._and(right));
			else if(node.operationToken.matches("or", TokenType.KEYWORD))	return res.register(left._or(right));
			
			if(res.shouldReturn()) return res;
			
			return res;
//			return res.failure(new Error.SyntaxError("Unknown symbol", node.operationToken.getSeq()));
		}
		
//		private Object visitBinaryOperation(BinaryOperation node, Context context) {
//			RTResult res = new RTResult();
//			
//			Object leftObj = res.register(visit(node.leftNode, context));
//			if(res.shouldReturn()) return res;
//			Value left = (Value) leftObj;
//			
//			Object rightObj = (Object)res.register(visit(node.rightNode, context));
//			if(res.shouldReturn()) return res;
//			Value right = (Value) rightObj;
//			
//				 if(node.operationToken.matches(TokenType.PLUS)) 	return res.success(left.add(right));
//			else if(node.operationToken.matches(TokenType.MINUS))	return res.success(left.sub(right));
//			else if(node.operationToken.matches(TokenType.MULT)) 	return res.success(left.mult(right));
//			else if(node.operationToken.matches(TokenType.DIV)) {
//				if(right instanceof Number)
//					left.seq = node.operationToken.getSeq();
//				Object o = left.div(right);
//				if(o instanceof Error) return res.failure((Error) o);
//				return res.success(o);
//			} else if(node.operationToken.matches(TokenType.DOUBLE_EQUALS))	return res.success(left._equals(right));
//			else if(node.operationToken.matches(TokenType.NOT_EQUALS)) 		return res.success(left._not_equals(right));
//			else if(node.operationToken.matches(TokenType.LESS)) 			return res.success(left._less(right));
//			else if(node.operationToken.matches(TokenType.LESS_EQUALS)) 	return res.success(left._less_equals(right));
//			else if(node.operationToken.matches(TokenType.GREATER)) 		return res.success(left._greater(right));
//			else if(node.operationToken.matches(TokenType.GREATER_EQUALS)) 	return res.success(left._greater_equals(right));
//			else if(node.operationToken.matches("and", TokenType.KEYWORD)) 	return res.success(left._and(right));
//			else if(node.operationToken.matches("or", TokenType.KEYWORD))	return res.success(left._or(right));
//			
//			return res.failure(new Error.SyntaxError("Unknown symbol", node.operationToken.getSeq()));
//		}
		
		private static final Number MINUS_ONE = new Number(-1);
		private Object visitUnaryOperation(UnaryOperation node, Context context) {
			RTResult res = new RTResult();
			
			if(debug) System.out.println("unary_node");
			Object obj = res.register(visit(node.node, context));
			if(res.shouldReturn()) return res;
			Number n = null;
			if(obj instanceof Number) n = (Number) obj; 
			else if(obj instanceof RTResult) n = (Number)((RTResult) obj).value;
			
			if(node.operationToken.matches(TokenType.MINUS)) n = (Number) n.mult(MINUS_ONE);
			else if(node.operationToken.matches("not", TokenType.KEYWORD)) n = (Number) n._not();
			
			return res.success(n);
		}
		
		private Object visitIfNode(IfNode node, Context context) {
			RTResult res = new RTResult();
			
			for(CaseDataNode cdn:node.cases) {
				Object condition = cdn.condition;
				Object expression = cdn.statements;
				
				Object condition_value = res.register(visit(condition, context));
				if(res.shouldReturn()) return res;
				
				if(((Number) condition_value).isTrue()) {
					Context newContext = new Context("<if>", context);
					Object expression_value = res.register(visit(expression, newContext));
					if(res.shouldReturn()) return res;
					
					return res.success(cdn.shouldReturnNull?Number.NULL:expression_value);
				}
			}
			
			if(node.else_case != null) {
				Context newContext = new Context("<if>", context);
				Object else_value = res.register(visit(node.else_case.statements, newContext));
				if(res.shouldReturn()) return res;
				return res.success(node.else_case.shouldReturnNull?Number.NULL:else_value);
			}
			
			return res.success(Number.NULL);
		}
		
		private Object visitForNode(ForNode node, Context context) {
			RTResult res = new RTResult();
			ArrayList<Object> elements = new ArrayList<Object>();
			
			Number start_value = (Number) res.register(visit(node.start, context));
			if(res.shouldReturn()) return res;
			
			Number end_value = (Number) res.register(visit(node.end, context));
			if(res.shouldReturn()) return res;
			
			Number step_value = new Number(start_value.value<end_value.value?1:-1);
			if(node.step != null) {
				step_value = (Number) res.register(visit(node.step, context));
				if(res.shouldReturn()) return res;
			}
			
			Context newContext = new Context("<for>", context);
			
			Number i = ((Number)start_value);
			
			while((step_value.value >= 0?i.value<((Number)end_value).value:i.value>((Number)end_value).value)) {
				newContext.set((String) node.varName.value, i);
				
				Object value = res.register(visit(node.body, newContext));
				if(res.shouldReturn() && !res.shouldContinue && !res.shouldBreak) return res;
				
				i = (Number) i.copy();
				i.value = i.value+step_value.value;
				
				if(res.shouldContinue) continue;
				if(res.shouldBreak) break;
				
				elements.add(value);
			}
			
			return res.success(node.shouldReturnNull?Number.NULL:new List(elements));
		}
		
		private Object visitForInNode(ForInNode node, Context context) {
			RTResult res = new RTResult();
			ArrayList<Object> elements = new ArrayList<Object>();
			
			Object objList = res.register(visit(node.array, context));
			if(res.shouldReturn()) return res;
			if(!(objList instanceof List))
				return res.failure(new Error.RuntimeError("'"+objList + "' is not a list.", null));
			ArrayList<Object> array = ((List) objList).elements; 
			
			Context newContext = new Context("<forin>", context);
			
			for(int i = 0; i < array.size(); i++) {
				newContext.set((String) node.varName.value, array.get(i));
				
				Object value = res.register(visit(node.body, newContext));
				if(res.shouldReturn() && !res.shouldContinue && !res.shouldBreak) return res;
				
				if(res.shouldContinue) continue;
				if(res.shouldBreak) break;
				
				elements.add(value);
			}
			
			return res.success(node.shouldReturnNull?Number.NULL:new List(elements));
		}
		
		private Object visitWhileNode(WhileNode node, Context context) {
			RTResult res = new RTResult();
			ArrayList<Object> elements = new ArrayList<Object>();
			
			Context newContext = new Context("<while>", context);
			
			while(true) {
				Object condition = res.register(visit(node.condition, context));
				if(res.shouldReturn()) return res;
				
				if(!((Number) condition).isTrue()) break;
				
				Object value = res.register(visit(node.body, newContext));
				
				if(res.shouldReturn() && !res.shouldContinue && !res.shouldBreak) return res;
				
				if(res.shouldContinue) continue;
				if(res.shouldBreak) break;
				
				elements.add(value);
			}
			
			return res.success(node.shouldReturnNull?Number.NULL:new List(elements));
		}
		
		private Object visitFunctionDefNode(FunctionDefNode node, Context context) {
			RTResult res = new RTResult();
			String fname = node.name==null?null:(String)node.name.value;
			String[] args_name = new String[node.args.length];
			for(int i = 0; i < args_name.length; i++) args_name[i] = (String) node.args[i].value;
			
			Object function = new Function(fname, node.body, args_name, node.shouldAutoReturn).setContext(context).setSeq(node.name==null?null:node.name.getSeq());
			
			if(node.name != null)
				context.set(fname, function);
			
			return res.success(function);
		}
		
		private Object visitCallNode(CallNode node, Context context) {
			RTResult res = new RTResult();
//			res.associatedToken = node.token;
			
//			System.out.println(">>>"+node.nodeToCall.getClass());
			
			Object obj = res.register(visit(node.nodeToCall, context));
			if(res.shouldReturn()) return res;
			
			if(!(obj instanceof BaseFunction)) return obj;
			BaseFunction value_to_call = (BaseFunction) obj;
			value_to_call.setContext(context);
			
			ArrayList<Value> args_value = new ArrayList<Value>();
			for(Object a:node.args) {
				a = res.register(visit(a, context));
				if(a instanceof Value) args_value.add((Value) a);
				else if(a instanceof RTResult) args_value.add((Value) ((RTResult) a).value);
				if(res.shouldReturn()) return res;
			}
			
			Object exe = res.register(value_to_call.execute(args_value.toArray(new Value[args_value.size()])));
			if(res.shouldReturn()) return res;
			
			Object return_value = res.register(exe);
			if(res.shouldReturn()) return res;
			
			return res.success(return_value);
		}
		
		private Object visitListNode(ListNode node, Context context) {
			RTResult res = new RTResult();
			ArrayList<Object> elements = new ArrayList<Object>();
			List l = new List(elements);
			l.generateContext(context);
			for(Object o:node.elementNodes) {
				elements.add(res.register(visit(o, l.context)));
				if(res.shouldReturn()) return res;
			}
			return res.success(l);
		}
		
		private Object visitStatementsNode(StatementsNode node, Context context) {
			RTResult res = new RTResult();
			for(Object o:node.elementNodes) {
//				if(o instanceof CallNode)
//					res.associated = ((CallNode) o).token.seq;
				res.register(visit(o, context));
				if(res.shouldReturn()) return res;
			}
			return res.success(new ObjectValue(context));
		}
		
		private Object visitReturnNode(ReturnNode node, Context context) {
			RTResult res = new RTResult();
			
			Object value = Number.NULL;
			if(node.toReturn != null) {
				value = res.register(visit(node.toReturn, context));
				if(res.shouldReturn()) return res;
			}
			
			return res.success_return(value);
		}
		
		private Object visitContinueNode(ContinueNode node, Context context) { return new RTResult().success_continue(); }
		private Object visitBreakNode(BreakNode node, Context context) { return new RTResult().success_break(); }
		
		private Object visitPointAccessNode(PointAccessNode node, Context context) {
			RTResult res = new RTResult();
			Object currentReturn = Number.NULL;
			Context currentContext = context;
			for(Object index:node.nodes) {
				Object value = res.register(visit(index, currentContext));
				if(res.shouldReturn()) return res;
				
//				System.out.println();
//				System.out.println("$"+value+"\n//"+currentContext);
				
				if(value instanceof RTResult)
					currentContext = ((Value)((RTResult)value).value).generateContext(currentContext);
				else
					currentContext = ((Value)value).generateContext(currentContext);
				
				//System.out.println("here " + ((Value)value).context);
				
				currentReturn = value;
			}
			return res.success(currentReturn);
		}
		
		private Object visitObjectDefNode(ObjectDefNode node, Context context) {
			RTResult res = new RTResult();
			
			String[] args_name = new String[node.args.length];
			for(int i = 0; i < args_name.length; i++)
				args_name[i] = (String) node.args[i].value;
			
			Object superClass = null;
			if(node.superClass != null)
				superClass = res.register(visit(node.superClass, context));
			if(res.shouldReturn())
				return res;
			ObjectClass oc = new ObjectClass((String) node.name.value, args_name, node.body, superClass);//node.superClass==null?null:context.get(node.superClass.value.toString()));
			context.set((String) node.name.value, oc);
			
			return res.success(oc);
		}
		
		private Object visitInstantiateNode(InstantiateNode node, Context context) {
			RTResult res = new RTResult();
			Object cl = res.register(visit(node.nodeToCall, context));
			if(res.shouldReturn()) return res;
			
			if(cl instanceof ObjectClass) {
				ArrayList<Value> args = new ArrayList<JIPL.Interpreter.Value>();
				
				for (Object obj:node.args) {
					obj = res.register(visit(obj, context));
					Value val = null;
					if(res.shouldReturn()) return res;
					else if(obj instanceof Value) val = ((Value) obj);
					else if(obj instanceof RTResult) val = ((Value)((RTResult) obj).value);
					args.add(val);
				}
				
				Object obj = res.register(((ObjectClass) cl).execute(context, args.toArray(new Value[args.size()])));
				if(res.shouldReturn()) return res;
				
				return res.success(obj);
			} else return res.failure(new Error.RuntimeError(((VarAccessNode)node.nodeToCall).name.getValue() + " is not an object.", null));
		}
		
		private Object visitIncludeNode(IncludeNode node, Context context) {
			RTResult res = new RTResult();
			
			Object path = res.register(visit(node.toInclude, context));
			
			File f = new File(new File(context.file).getParent()+"/"+path);
			Context c = JIPL.run(f, context);
			context.symbols.putAll(c.symbols);
			context.getterTracks.putAll(c.getterTracks);
			context.setterTracks.putAll(c.setterTracks);
			
			return res.success(new ObjectValue(c));
		}
		
	}
	
	public static class Context implements Serializable {
		
		private static final long serialVersionUID = 1L;
		public String displayName, file;
		public Context parent = null;

		private HashMap<String, Object> symbols;
		private HashMap<String, Consumer<Object>> setterTracks = new HashMap<>();
		private HashMap<String, Callable<Object>> getterTracks = new HashMap<>();
		
		public String toString() {
			return displayName;
		}
		
		public Context(String displayName) {
			this(displayName, null);
		}
		
		public Context(String displayName, Context parent) {
			this.displayName = displayName;
			this.parent = parent;
			this.symbols = new HashMap<>();
			this.setterTracks = new HashMap<>();
			this.getterTracks = new HashMap<>();
			this.file = parent!=null?parent.file:null;
		}
		
		public void newParent(Context parent) {
			if(parent == this) return;
			if(parent.parent == this) return;
			if(this.parent != null)
				this.parent = parent;
		}
		
		public void set(String name, Object value) {
			symbols.put(name, value);
			if(setterTracks.containsKey(name))
				try {
					setterTracks.get(name).accept(Context.getAppropriateObject(value));
				} catch (Exception e) { e.printStackTrace(); }
		}
		
		public void set(String name, Object value, Consumer<Object> setterTrack, Callable<Object> getterTrack) {
			symbols.put(name, value);
			setterTracks.put(name, setterTrack);
			getterTracks.put(name, getterTrack);
		}
		
		public void setAppropriateObject(String name, Object o) {
			symbols.put(name, appropriateObjectConverter(o));
		}
		
		@SuppressWarnings("unchecked")
		public static Object appropriateObjectConverter(Object o) {
			if(o instanceof Integer || o instanceof Float || o instanceof Double) o = new Number(o);
			else if(o instanceof Boolean) o = new Number((boolean) o?Number.TRUE:Number.FALSE);
			else if(o instanceof String) o = new StringValue(o);
			else if(o instanceof ArrayList) o = new List((ArrayList<Object>) o);
			else if(o == null) o = Number.NULL;
			return o;
		}
		
		public Object get(String name) {
			if(symbols.containsKey(name)) {
				if(getterTracks.containsKey(name))
					try { symbols.put(name, Context.appropriateObjectConverter(getterTracks.get(name).call())); } catch (Exception e) { e.printStackTrace(); }
				return symbols.get(name);
			} else if(parent != null && parent != this && parent.parent != this) {
//				System.out.println(parent);
				System.out.println(parent + "->" + name);
				return parent.get(name);
			}
			
			return Number.NULL;
		}
		
		public Context getSource(String name) {
			if(symbols.containsKey(name)) return this;
//			System.out.println(parent);
			if(parent != null && parent != this) return parent.getSource(name);
			return this;
		}
		
		public Context getSource(String name, Context def) {
			if(symbols.containsKey(name)) return this;
			if(parent != null) return parent.getSource(name);
			return def;
		}
		
//		public Object get(String name) { // TODO VERY EXPERIMENTAL
//			return symbolTable.get(name);
////			if(symbolTable.symbols.containsKey(name)) return symbolTable.symbols.get(name);
////			else if(parent != null && this != parent && parent.get(name) != Number.NULL) {
////				return parent.get(name);
////			}
////			return Number.NULL;
//		}
		
		public boolean hasAsParent(Context con) {
//			if(this == con) return true;
//			System.out.println(con);
//			System.out.println(parent);
//			if(parent != null) System.out.println(parent.parent);
//			System.out.println(parent);
			return parent != null && (parent != con || parent.hasAsParent(con));
		}
		
//		public boolean isContextLined() {
//			return false;
//		}
		
		public Object getAppropriateObject(String name) {
			Object o = get(name);
			return getAppropriateObject(o);
		}
		
		public static Object getAppropriateObject(Object o) {
			if(o instanceof Number) o = ((Number) o).value;
			else if(o instanceof StringValue) o = ((StringValue) o).value;
			else if(o instanceof List) o = getAppropriateList(((List) o).elements);
			return o;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public static ArrayList getAppropriateList(ArrayList array) {
			for(int i = 0; i < array.size(); i++) {
				Object l = array.get(i);
				array.set(i, getAppropriateObject(l));
			}
			return array;
		}

		public HashMap<String, Object> getSymbols() { return symbols; }
		
		public void remove(String name) { symbols.remove(name); }
		
		public float number(String name) { return ((Number)get(name)).value; }
		public String string(String name) { return ((StringValue)get(name)).value; }
		public ArrayList<Object> list(String name) { return ((List)get(name)).elements; }
		public Function function(String name) { return (Function)get(name); }
		public ObjectValue object(String name) { return (ObjectValue)get(name); }

		public Object strictget(String name) { return symbols.get(name); }
	}
	
//	public static class SymbolTable implements Serializable {
//		
//		private static final long serialVersionUID = 1L;
//		private HashMap<String, Object> symbols;
//		private HashMap<String, Consumer<Object>> setterTracks = new HashMap<>();
//		private HashMap<String, Callable<Object>> getterTracks = new HashMap<>();
//		private SymbolTable parent = null;
//		
//		public SymbolTable(SymbolTable parent) {
//			this.symbols = new HashMap<String, Object>();
//			this.parent = parent;
//		}
//		
//		public Object get(String name) {
//			if(symbols.containsKey(name)) {
//				if(getterTracks.containsKey(name))
//					try { symbols.put(name, Context.appropriateObjectConverter(getterTracks.get(name).call())); } catch (Exception e) { e.printStackTrace(); }
//				return symbols.get(name);
//			}
//			if(parent != null && parent != this && (parent.parent != this)) return parent.get(name);
//			return Number.NULL;
//		}
//		
//		public SymbolTable getSource(String name) {
//			if(symbols.containsKey(name)) return this;
//			if(parent != null) return parent.getSource(name);
//			return this;
//		}
//		private SymbolTable getSourcePrime2(String name) {
//			if(symbols.containsKey(name)) return this;
//			if(parent != null) return parent.getSourcePrime2(name);
//			return null;
//		}
//		
//		public SymbolTable getSourcePrime(String name) {
//			if(symbols.containsKey(name)) return this;
//			SymbolTable tb = parent==null?null:parent.getSourcePrime2(name);
//			if(tb == null) return this;
//			return tb;
//		}
//		
//		
//		public void set(String name, Object value) {
//			symbols.put(name, value);
//			if(setterTracks.containsKey(name))
//				try {
//					setterTracks.get(name).accept(Context.getAppropriateObject(value));
//				} catch (Exception e) { e.printStackTrace(); }
//		}
//		public void set(String name, Object value, Consumer<Object> setterTrack, Callable<Object> getterTrack) {
//			symbols.put(name, value);
//			setterTracks.put(name, setterTrack);
//			getterTracks.put(name, getterTrack);
//		}
//		public void remove(String name) { symbols.remove(name); }
//
//		public HashMap<String, Object> getSymbols() { return symbols; }
//		public void setSymbols(HashMap<String, Object> symbols) { this.symbols = symbols; }
//		
//	}
	
	public static class Sequence implements Serializable {
		
		private static final long serialVersionUID = 1L;
		private int line, offset, size;
		private String file;
		
//		public Sequence(int line, int offset, int size) {
//			this.line = line+1;
//			this.offset = offset;
//			this.size = size;
//		}
		
		public Sequence(int line, int offset, int size, String file) {
			this.line = line+1;
			this.offset = offset;
			this.size = size;
			this.file = file;
		}
		
		public int getLine() { return line; }
		public void setLine(int line) { this.line = line; }

		public int getOffset() { return offset; }
		public void setOffset(int offset) { this.offset = offset; }

		public int getSize() { return size; }
		public void setSize(int size) { this.size = size; }
		
		public String toString() {
//			if(line == 0) return "["+offset+"]";
			return "line "+line + "("+file+")";
		}
		
		public String apply(String text) { return text.substring(offset, offset+size); }
		
	}
	
	public static final class Token {
		
		protected TokenType type;
		protected Object value;
		protected Sequence seq;
		
		public Token(TokenType type, Sequence seq, Object value) {
			this.type = type;
			this.seq = seq;
			this.value = value;
		}
		
		public Token(TokenType type, Sequence seq) {
			this.type = type;
			this.seq = seq;
		}
		
		public TokenType getType() { return type; }
		public void setType(TokenType type) { this.type = type; }
		
		public Sequence getSeq() { return seq; }
		public void setSeq(Sequence seq) { this.seq = seq; }

		public Object getValue() { return value; }
		public void setValue(Object value) { this.value = value; }
		
		public boolean matches(TokenType type) { return this.type == type; }
		public boolean matches(String value, TokenType type) { return this.type == type && this.value.equals(value); }
		public boolean matches(TokenType... types) { 
			for(TokenType tt:types)
				if(type == tt) return true;
			return false;
		}
		
		public String toString() { if(value == null) return type+""; return type+":"+value; }
		
	}
	
	public static enum TokenType {
		INT, FLOAT, STRING,
		PLUS, MINUS, MULT, DIV, POWER,
		DPLUS, DMINUS, PLUS_EQUAL, MINUS_EQUAL,
		EQUALS, DOUBLE_EQUALS, NOT_EQUALS, LESS, GREATER, LESS_EQUALS, GREATER_EQUALS, LPAREN, RPAREN, LSQUARE, RSQUARE, LBRA, RBRA,
		COLON, COMMAS, POINT, NLINE,
		KEYWORD, IDENTIFIER,
		END_OF_CODE;
	}
	
	public static class Network {
		public ArrayList<ServerClient> clients;
		
		public Context context;
		public Thread t;
		@SuppressWarnings("deprecation")
		public void open(int port, Context context) throws Exception {
			if(t != null)
				t.stop();
				t = new Thread(new Runnable() {
				public void run() {
					ServerSocket server;
					try {
						server = new ServerSocket(port);
						clients = new ArrayList<Network.ServerClient>();
						System.out.println("Server started on port " + port);
						while(true) {
							try {
								Socket sock = server.accept();
								ServerClient c = new ServerClient(sock);
								clients.add(c);
								c.start();
								for(ServerClient sc:clients) {
									if(sc != c) {
//										System.out.println(sc.cname);
										c.send(new Client.AddConnectionPacket(sc.cname));
									}
								}
								System.out.println("Socket connected : " + sock.getInetAddress());
							} catch(Exception e) { e.printStackTrace(); }
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}, "network");
			t.start();
		}
		
		@SuppressWarnings("deprecation")
		public void close() {
			t.stop();
		}
		
		public void interpret(Object obj, ServerClient sender) {
			if(obj instanceof Client.AddConnectionPacket) {
				sender.cname = ((Client.AddConnectionPacket) obj).name;
				sendToAll(obj);
			} else if(obj instanceof Client.RemoveConnectionPacket) {
				clients.remove(sender);
				sendToAll(obj);
			} else sendToAllExcept(Context.appropriateObjectConverter(obj), sender);
		}
		
		public void sendToAll(Object obj) {
			for(ServerClient sc:clients)
				sc.send(obj);
		}
		
		public void sendToAllExcept(Object obj, ServerClient c) {
			for(ServerClient sc:clients)
				if(c!=sc) sc.send(obj);
		}
		
		public class ServerClient extends Thread {
			
			private Socket socket;
			private ObjectOutputStream out;
			private ObjectInputStream in;
			
			public String cname = "Anonymous";
			
			public ServerClient(Socket socket) {
				this.socket = socket;
				try {
					out = new ObjectOutputStream(socket.getOutputStream());
					in = new ObjectInputStream(socket.getInputStream());
				} catch(IOException e) { e.printStackTrace(); }
			}
			
			public void run() {
				while(true) {
					try {
						Object obj = in.readObject();
						interpret(obj, this);
					} catch (ClassNotFoundException | IOException e) {
//						e.printStackTrace();
						System.out.println(socket + " disconnected.");
						clients.remove(this);
						sendToAll(new Client.RemoveConnectionPacket(cname));
						break;
					}
				}
			}
			
			public void send(Object obj) {
				obj = Context.getAppropriateObject(obj);
				try { out.writeObject(obj); }
				catch (IOException e) { e.printStackTrace(); System.err.println(socket); }
			}
			
			public Socket getSocket() { return socket; }
			public ObjectOutputStream getOut() { return out; }
			public ObjectInputStream getIn() { return in; }
		}
	}
	
	public static class Client {
		
		private String name;
		
		private Socket socket;
		public ObjectOutputStream out;
		public ObjectInputStream in;
		
		public Context context;
		
		public static Client connect(String name, String ip, int port, Context context) {
			try {
				Socket socket = new Socket(InetAddress.getByName(ip), port);
				Client client = new Client(name, socket, context);
				return client;
			} catch (IOException e) { e.printStackTrace(); }
			return null;
		}
		
		public Client(String name, Socket socket, Context context) {
			this.name = name;
			this.socket = socket;
			this.context = context;
			try {
				this.out = new ObjectOutputStream(socket.getOutputStream());
				this.in = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) { e.printStackTrace(); }
		}
		
		public void start() {
			new Thread("Client") {
				public void run() {
					while(true) {
						try {
							Object obj = in.readObject();
							receive(obj);
						} catch (ClassNotFoundException | IOException e) { e.printStackTrace(); }
					}
				}
			}.start();
		}
		
		public void receive(Object obj) {
			obj = Context.appropriateObjectConverter(obj);
			Object receive = context.get("receive");
//			System.out.println("received:::"+obj);
			if(obj instanceof Client.AddConnectionPacket) {
				Object connect = context.get("connect");
				if(connect != null && connect instanceof Function) {
					Function fjoin = (Function) connect;
					fjoin.execute(new StringValue(((AddConnectionPacket)obj).name));
				}
			} else if(obj instanceof Client.RemoveConnectionPacket) {
				Object disconnect = context.get("disconnect");
				if(disconnect != null && disconnect instanceof Function) {
					Function fdisconnect = (Function) disconnect;
					fdisconnect.execute(new StringValue(((RemoveConnectionPacket)obj).name));
				}
			} else if(receive != null && receive instanceof Function) {
				Function freceive = (Function) receive;
				freceive.execute((Value) obj);
			}
		}

		public boolean send(Object obj) {
			try { out.writeObject(Context.getAppropriateObject(obj)); return true;
			} catch (IOException e) { e.printStackTrace(); return false; }
		}
		
		public boolean send(String type, Object content) {
			try { out.writeObject(new Packet(name, type, content)); return true;
			} catch (IOException e) { e.printStackTrace(); return false; }
		}
		
		public Socket getSocket() { return socket; }
		public ObjectOutputStream getOut() { return out; }
		public ObjectInputStream getIn() { return in; }

		public String getName() { return name; }
		public void setName(String name) { this.name = name; }

		public static class AddConnectionPacket implements Serializable { private static final long serialVersionUID = 1L;
			public String name;
			public AddConnectionPacket(String name) { this.name = name; }
			public String toString() { return name + " connected."; }
		}

		public static class RemoveConnectionPacket implements Serializable { private static final long serialVersionUID = 1L;
			public String name;
			public RemoveConnectionPacket(String name) { this.name = name; }
			public String toString() { return name + " disconnected."; }
		}
		
		public static class Packet implements Serializable { private static final long serialVersionUID = 1L;
			public String name, type;
			public Object content;
			public Packet(String name, String type, Object content) { this.name = name; this.type = type; this.content = content; }
			public String toString() { return name+" > "+type+" : "+content; }
		}
		
	}
	
}