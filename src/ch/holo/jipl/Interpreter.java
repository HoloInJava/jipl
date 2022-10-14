package ch.holo.jipl;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import ch.holo.jipl.Error.IllegalArgumentError;
import ch.holo.jipl.Error.RuntimeError;
import ch.holo.jipl.Parser.BinaryOperation;
import ch.holo.jipl.Parser.BreakNode;
import ch.holo.jipl.Parser.CallNode;
import ch.holo.jipl.Parser.CaseDataNode;
import ch.holo.jipl.Parser.ContinueNode;
import ch.holo.jipl.Parser.ForInNode;
import ch.holo.jipl.Parser.ForNode;
import ch.holo.jipl.Parser.FunctionDefNode;
import ch.holo.jipl.Parser.IfNode;
import ch.holo.jipl.Parser.IncludeNode;
import ch.holo.jipl.Parser.InstantiateNode;
import ch.holo.jipl.Parser.ListNode;
import ch.holo.jipl.Parser.NumberNode;
import ch.holo.jipl.Parser.ObjectDefNode;
import ch.holo.jipl.Parser.PointAccessNode;
import ch.holo.jipl.Parser.ReturnNode;
import ch.holo.jipl.Parser.StatementsNode;
import ch.holo.jipl.Parser.StringNode;
import ch.holo.jipl.Parser.ThisNode;
import ch.holo.jipl.Parser.UnaryOperation;
import ch.holo.jipl.Parser.VarAccessNode;
import ch.holo.jipl.Parser.VarAddNode;
import ch.holo.jipl.Parser.VarAssignNode;
import ch.holo.jipl.Parser.VarDivNode;
import ch.holo.jipl.Parser.VarModifyNode;
import ch.holo.jipl.Parser.VarMultNode;
import ch.holo.jipl.Parser.VarSubNode;
import ch.holo.jipl.Parser.WhileNode;
import ch.holo.jipl.Token.TokenType;

public class Interpreter {
	
	public static class Value implements Serializable {
		private static final long serialVersionUID = 1L;
		
		protected Context context;
		protected Sequence seq;
		
		protected Error.RuntimeError illegal_operation(Object obj) { return new RuntimeError("Illegal operation with " + obj, seq); }
		
		protected Object execute(Value... args) { System.err.println("No execution defined for " + this); return null; }
		public Value copy() { return this; }
		
		public Context getContext() { return context; }
		
		public Value setContext(Context context) { // TODO: review this
			if(context == this.context)
				return this;
			if(this.context != null)
				this.context.newParent(context);
			else this.context = context;
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
				if(n.value == 0) return new Error.RuntimeError("Division by zero", n.seq);
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
					StringValue[] chars = new StringValue[value.length()];
					for(int i = 0; i < chars.length; i++)
						chars[i] = new StringValue(value.charAt(i)+"");
					return res.success(new List(chars));
				}
			});
			selfContext.set("substring", new BuildInFunction("sub", "start", "end") {
				private static final long serialVersionUID = 1L;
				protected Object executeFunction(Context context, RTResult res, Value... args) {
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
			new_context.set("type", this);
			
			if(superClass instanceof ObjectClass) {
				new_context.set("super", new BuildInFunction("super", ((ObjectClass) superClass).args_name) {
					private static final long serialVersionUID = 1L;
					protected Object executeFunction(Context context, RTResult res, Value... args) {
						Object extend = res.register(((ObjectClass) superClass).execute(context, args));
						
						if(res.error != null)
							return res;
						
						if(extend instanceof ObjectValue)
							new_context.symbols.putAll(((ObjectValue) extend).selfContext.symbols);
						
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
			return "Object("+selfContext+")";
		}
	}
	
	public static class RTResult {
		
		public Object value = null, returnValue = null;
		public Error error = null;
		public boolean shouldContinue = false, shouldBreak = false;
		public Token associatedToken;
		
		public void reset() {
			error = null;
			value = null;
			returnValue = null;
			shouldContinue = false;
			shouldBreak = false;
		}
		
		public Object register(Object value) {
			if(value instanceof RTResult) {
				RTResult pr = (RTResult) value;
				if(pr.error != null) {
					if(associatedToken != null)
						pr.error.add(associatedToken);
					error = pr.error;
					associatedToken = pr.associatedToken;
					return this;
				}
				returnValue = pr.returnValue;
				shouldContinue = pr.shouldContinue;
				shouldBreak = pr.shouldBreak;
				associatedToken = pr.associatedToken;
				return pr.value;
			}
			if(value instanceof Error) {
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
			return "RTResult("+value+", "+error+")";
		}
	}
	
	public Object visit(Object node, Context context) {
		if(JIPL.debug) System.out.println("Intepreter: Visit " + node); //+ "["+context+"]");
		if(JIPL.stop) return new RTResult().failure(new Error.Stop("Stop", null));
		
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
		else if(node instanceof VarMultNode) 		return visitVarMultNode		((VarMultNode) node, 		context);
		else if(node instanceof VarDivNode) 		return visitVarDivNode		((VarDivNode) node,			context);
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
		
		System.out.println(node);
		System.err.println("Intepreter: No visit for " + node + ".");
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
//			System.out.println("CON " + context + " " + context.symbols);
			return res.failure(new Error.NullPointerError(vname + " is not defined", node.name.getSeq()));
		}
		
		return res.success(value);
	}
	
	private Object visitThisNode(ThisNode node, Context context) {
		RTResult res = new RTResult();
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
			Context con = context.getSource(name);
			con.set(name, ((Value) con.get(name)).add(value));
			
			return con.get(name);
		}
		
		return res.success(value);
	}
	
	private Object visitVarSubNode(VarSubNode node, Context context) {
		RTResult res = new RTResult();
		
		String name = (String) node.name.value;
		Object value = res.register(visit(node.node, context));
		if(res.shouldReturn()) return res;
		
		if(!name.equals("this")) {
			Context con = context.getSource(name);
			con.set(name, ((Value) con.get(name)).sub(value));
			
			return res.success(con.get(name));
		}
		
		return res.success(value);
	}
	
	private Object visitVarMultNode(VarMultNode node, Context context) {
		RTResult res = new RTResult();
		
		String name = (String) node.name.value;
		Object value = res.register(visit(node.node, context));
		if(res.shouldReturn()) return res;
		
		if(!name.equals("this")) {
			Context con = context.getSource(name);
			con.set(name, ((Value) con.get(name)).mult(value));
			
			return res.success(con.get(name));
		}
		
		return res.success(value);
	}
	
	private Object visitVarDivNode(VarDivNode node, Context context) {
		RTResult res = new RTResult();
		
		String name = (String) node.name.value;
		Object value = res.register(visit(node.node, context));
		if(res.shouldReturn()) return res;
		
		if(!name.equals("this")) {
			Context con = context.getSource(name);
			Object val = res.register(((Value) con.get(name)).div(value));
			if(res.shouldReturn()) return res;
			
			con.set(name, val);
			
			return res.success(con.get(name));
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
		else if(node.operationToken.matches(TokenType.DIV)) 	return res.register(left.div(right));
		else if(node.operationToken.matches(TokenType.DOUBLE_EQUALS))	return res.register(left._equals(right));
		else if(node.operationToken.matches(TokenType.NOT_EQUALS)) 		return res.register(left._not_equals(right));
		else if(node.operationToken.matches(TokenType.LESS)) 			return res.register(left._less(right));
		else if(node.operationToken.matches(TokenType.LESS_EQUALS)) 	return res.register(left._less_equals(right));
		else if(node.operationToken.matches(TokenType.GREATER)) 		return res.register(left._greater(right));
		else if(node.operationToken.matches(TokenType.GREATER_EQUALS)) 	return res.register(left._greater_equals(right));
		else if(node.operationToken.matches("and", TokenType.KEYWORD)) 	return res.register(left._and(right));
		else if(node.operationToken.matches("or", TokenType.KEYWORD))	return res.register(left._or(right));
		
		return res;
	}
	
	private static final Number MINUS_ONE = new Number(-1);
	private Object visitUnaryOperation(UnaryOperation node, Context context) {
		RTResult res = new RTResult();
		
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
				Context newContext = new Context("<if "+condition+">", context);
				Object expression_value = res.register(visit(expression, newContext));
				if(res.shouldReturn()) {
					//System.out.println("   Should return.");
					return res;
				}
				
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
		
		Context newContext = new Context("<forin "+node.varName.value+">", context);
		
		for(int i = 0; i < array.size(); i++) {
			newContext.set((String) node.varName.value, array.get(i));
			
			Object value = res.register(visit(node.body, newContext));
			if(res.shouldReturn() && !res.shouldContinue && !res.shouldBreak) {
				//System.out.println("  For: should return");
				return res;
			}
			
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
			
			if(value instanceof RTResult)
				currentContext = ((Value)((RTResult)value).value).generateContext(currentContext);
			else
				currentContext = ((Value)value).generateContext(currentContext);
			
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
			ArrayList<Value> args = new ArrayList<Interpreter.Value>();
			
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
		if(node.toInclude.length == 1)
			return singleInclude(node.toInclude[0], context);
		
		RTResult res = new RTResult();
		for(int i = 0; i < node.toInclude.length; i++) {
			res.register(singleInclude(node.toInclude[i], context));
			if(res.shouldReturn()) return res;
//			String path = res.register(visit(node.toInclude, context)).toString();
//			File f = new File(new File(context.file).getParent()+"/"+path);
//			if(path.endsWith("/")) {
//				for(File file:f.listFiles()) {
//					Context con = JIPL.run(file, context);
//					g.putAll(con);
//				}
//				context.putAll(g);
//			} else {
//				context.putAll(JIPL.run(f, context));
//			}
		}
		return res.success(Number.NULL);
	}
	
	private Object singleInclude(Object node, Context context) {
		RTResult res = new RTResult();
		
		String path = res.register(visit(node, context)).toString();
		if(res.shouldReturn()) return res;
		
		File file = new File(new File(context.file).getParent()+"/"+path);
		
		if(!file.exists())
			return res.failure(new RuntimeError("File " + file.getName() + " not found.", null));
		
		Context include = new Context(path, context);
		if(path.endsWith("/")) {
			for(File f:file.listFiles()) {
				Context con = JIPL.run(f, context);
				include.putAll(con);
			}
		} else include = JIPL.run(file, context);
		
		context.putAll(include);
		
		return res.success(new ObjectValue(include));
	}
	
}
