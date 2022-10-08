package ch.holo.jipl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import ch.holo.jipl.Error.IllegalArgumentError;
import ch.holo.jipl.Interpreter.BuildInFunction;
import ch.holo.jipl.Interpreter.BuildInObjectClass;
import ch.holo.jipl.Interpreter.Function;
import ch.holo.jipl.Interpreter.List;
import ch.holo.jipl.Interpreter.Number;
import ch.holo.jipl.Interpreter.RTResult;
import ch.holo.jipl.Interpreter.StringValue;
import ch.holo.jipl.Interpreter.Value;

public abstract class JIPLModule {
	
	public static final JIPLModule BUILT_IN_FUNCTIONS = new JIPLModule() {
		public Context generate(Context con, Object... data) {
			con.set("print", new BuildInFunction("print", "text") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					System.out.println(""+args[0]);
					return res.success(Number.NULL);
				}
			});
			
			con.set("jipl", new BuildInFunction("jipl", "code") {
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
			
			con.set("wait", new BuildInFunction("wait", "value") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number) {
						try { Thread.sleep((long) ((Number)args[0]).value);
						} catch (InterruptedException e) { e.printStackTrace(); }
					} else return res.failure(new Error.RuntimeError("Invalid argument type, "+args[0]+" is not allowed to the function '"+name+"'", seq));
					return res.success(Number.NULL);
				}
			});
			
			con.set("d_alloc", new BuildInFunction("d_alloc", "var") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					context.parent.remove(args[0].toString());
					return res.success(Number.NULL);
				}
			});
			
			con.set("async", new BuildInFunction("async", "fun") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Function) {
						new Thread(() -> {
							args[0].execute();
						}).start();
						return res.success(Number.NULL);
					} else return res.failure(new Error.RuntimeError("Invalid argument type, "+args[0]+" is not allowed to the function '"+name+"'", seq));
				}
			});
			
			return con;
		}
	};

	public static final JIPLModule MATHS_FUNCTIONS = new JIPLModule() {
		public Context generate(Context con, Object... data) {
			con.set("sin", new BuildInFunction("sin", "value") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number) return res.success(new Number((float)Math.sin(((Number) args[0]).value)));
					return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
				}
			});
			
			con.set("cos", new BuildInFunction("cos", "value") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number) return res.success(new Number((float)Math.cos(((Number) args[0]).value)));
					return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
				}
			});
			
			con.set("min", new BuildInFunction("min", "a", "b") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number && args[1] instanceof Number)
						return res.success(new Number(Math.min(((Number)args[0]).value, ((Number)args[1]).value)));
					
					if(!(args[0] instanceof Number))
						return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
					
					return res.failure(new IllegalArgumentError(name, args[1], args[1].seq));
				}
			});
			
			con.set("max", new BuildInFunction("max", "a", "b") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number && args[1] instanceof Number)
						return res.success(new Number(Math.max(((Number)args[0]).value, ((Number)args[1]).value)));
					
					if(!(args[0] instanceof Number))
						return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
					
					return res.failure(new IllegalArgumentError(name, args[1], args[1].seq));
				}
			});
			
			con.set("clamp", new BuildInFunction("clamp", "value", "min", "max") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					res.register(checkArgumentTypes(res, args, Number.class, Number.class, Number.class));
					if(res.shouldReturn()) return res;
					return res.success(new Number(Math.max(context.number("min"), Math.min(context.number("max"), context.number("value")))));
				}
			});
			
			con.set("signum", new BuildInFunction("signum", "value") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number) return res.success(new Number((float)Math.signum(((Number) args[0]).value)));
					return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
				}
			});
			
			con.set("abs", new BuildInFunction("abs", "value") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number) return res.success(new Number((float)Math.abs(((Number) args[0]).value)));
					return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
				}
			});
			
			con.set("floor", new BuildInFunction("floor", "value") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number) return res.success(new Number((float)Math.floor(((Number) args[0]).value)));
					return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
				}
			});
			
			con.set("ceil", new BuildInFunction("ceil", "value") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number) return res.success(new Number((float)Math.ceil(((Number) args[0]).value)));
					return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
				}
			});
			
			con.set("toRadians", new BuildInFunction("toRadians", "value") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number) return res.success(new Number((float)Math.toRadians(((Number) args[0]).value)));
					return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
				}
			});
			
			con.set("toDegrees", new BuildInFunction("toDegrees", "value") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number) return res.success(new Number((float)Math.toDegrees(((Number) args[0]).value)));
					return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
				}
			});
			
			con.set("random", new BuildInFunction("random") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					return res.success(new Number((float)Math.random()));
				}
			});
			
			con.set("randomBetween", new BuildInFunction("randomBetween", "min", "max") {
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
			
			con.set("sqrt", new BuildInFunction("sqrt", "value") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					if(args[0] instanceof Number) return res.success(new Number((float)Math.sqrt(((Number) args[0]).value)));
					return res.failure(new IllegalArgumentError(name, args[0], args[0].seq));
				}
			});
			
			con.set("distance", new BuildInFunction("distance", "x1", "y1", "x2", "y2") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					float[] values = new float[4];
					for(int i = 0; i < values.length; i++)
						if(args[i] instanceof Number) values[i] = ((Number) args[i]).value;
						else return res.failure(new IllegalArgumentError(name, args[i], args[i].seq));
					return res.success(new Number((float)Math.sqrt((values[0]-values[2])*(values[0]-values[2])+(values[1]-values[3])*(values[1]-values[3]))));
				}
			});
			
			con.set("modulo", new BuildInFunction("modulo", "value", "diviser") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					float[] values = new float[2];
					for(int i = 0; i < values.length; i++)
						if(args[i] instanceof Number) values[i] = ((Number) args[i]).value;
						else return res.failure(new IllegalArgumentError(name, args[i], args[i].seq));
					return res.success(new Number(values[0]%values[1]));
				}
			});
			
			return con;
		}
	};

	public static final JIPLModule IO_FUNCTIONS = new JIPLModule() {
		public Context generate(Context st, Object... data) {
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
					
					st.set("exists", new BuildInFunction("exists") {
						private static final long serialVersionUID = 1L;
						protected Object executeFunction(Context context, RTResult res, Value... args) {
							return res.success(f.exists()?Number.TRUE:Number.FALSE);
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
			
			return st;
		}
	};

	public static final JIPLModule SCANNER_FUNCTIONS = new JIPLModule() {
		public Context generate(Context con, Object... data) {
			@SuppressWarnings("resource")
			Scanner globalScanner = new Scanner(System.in);
			con.set("readString", new BuildInFunction("readString") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					System.out.print("> ");
					return res.success(new StringValue(globalScanner.nextLine()));
				}
			});
			con.set("readNumber", new BuildInFunction("readNumber") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
					System.out.print("> ");
					
					return res.success(new Number(globalScanner.nextInt()));
				}
			});
			return con;
		}
	};
	
	public abstract Context generate(Context con, Object... data);
	
}
