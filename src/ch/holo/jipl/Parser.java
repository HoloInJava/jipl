package ch.holo.jipl;

import java.util.ArrayList;

import ch.holo.jipl.Error.SyntaxError;
import ch.holo.jipl.Interpreter.Number;
import ch.holo.jipl.Token.TokenType;

public class Parser {
	
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
		
		public String toString() { return "if " + cases + ": " + else_case + ""; }
		
	}
	
	public static class VarAssignNode {
		
		protected Token name;
		protected Object expression;
		
		public VarAssignNode(Token name, Object expression) {
			this.name = name;
			this.expression = expression;
		}
		
		public String toString() { return ""+name+" = "+expression; }
	}
	
	public static class VarAccessNode {
		public Token name;
		public VarAccessNode(Token name) { this.name = name; }
		public String toString() { return name.getValue().toString(); }
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
		
		public String toString() { return name+" = "+node; }
	}
	
	public static class VarAddNode {
		protected Token name;
		protected Object node;
		
		public VarAddNode(Token name, Object node) {
			this.name = name;
			this.node = node;
		}
		
		public String toString() { return name+" += "+node; }
	}
	
	public static class VarSubNode {
		protected Token name;
		protected Object node;
		
		public VarSubNode(Token name, Object node) {
			this.name = name;
			this.node = node;
		}
		
		public String toString() { return ""+name+" -= "+node+""; }
	}
	
	public static class VarMultNode {
		protected Token name;
		protected Object node;
		
		public VarMultNode(Token name, Object node) {
			this.name = name;
			this.node = node;
		}
		
		public String toString() { return ""+name+" *= "+node+""; }
	}
	
	public static class VarDivNode {
		protected Token name;
		protected Object node;
		
		public VarDivNode(Token name, Object node) {
			this.name = name;
			this.node = node;
		}
		
		public String toString() { return ""+name+" /= "+node+""; }
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
		
		public String toString() { return "for " + varName.value.toString() + " = " + start +" to "+end+": "+body; }
		
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
		
		public String toString() { return "for "+varName+" in "+array+": "+body; }
		
	}
	
	public static class WhileNode {
		
		protected Object condition, body;
		protected boolean shouldReturnNull;
		
		public WhileNode(Object condition, Object bodyNode, boolean shouldReturnNull) {
			this.condition = condition;
			this.body = bodyNode;
			this.shouldReturnNull = shouldReturnNull;
		}
		
		public String toString() { return "while "+condition+": "+body; }
		
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
	
	public static class ContinueNode { public String toString() { return "continue"; } }
	public static class BreakNode { public String toString() { return "break"; } }
	
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
	
//	public static class IncludeNode {
//		
//		protected Object toInclude;
//		public IncludeNode(Object toInclude) {
//			this.toInclude = toInclude;
//		}
//		
//	}
	
	public static class IncludeNode {
		
		protected Object[] toInclude;
		public IncludeNode(Object... toInclude) {
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
			last_registered_advance = pr.advance_count;
			advance_count += pr.advance_count;
			if(pr.error != null) error = pr.error;
			return pr.node;
		}
		
		public Object try_register(ParseResult node) {
			if(node.error != null) {
				reverse_count = node.advance_count;
				if(node.error != null && reverse_count >= 1) error = node.error;
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
		debug("Parser: Reversing "+amount);
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
			} else if(currentToken.matches(TokenType.MULT_EQUAL)) {
				pr.register_advancement();
				advance();
				Object o = pr.register(expression());
				if(pr.error != null) return pr;
				return pr.success(new VarMultNode(t, o));
			} else if(currentToken.matches(TokenType.DIV_EQUAL)) {
				pr.register_advancement();
				advance();
				Object o = pr.register(expression());
				if(pr.error != null) return pr;
				return pr.success(new VarDivNode(t, o));
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
			
			ArrayList<Object> toInclude = new ArrayList<>();
			do {
				if(currentToken.matches(TokenType.COMMAS)) {
					pr.register_advancement();
					advance();
					advanceNewLines(pr);
				}
				Object expr = pr.register(expression());
				if(pr.error != null) return pr;
				toInclude.add(expr);
			} while(currentToken.matches(TokenType.COMMAS));
			
			return pr.success(new IncludeNode(toInclude.toArray()));
		} else if(t.matches(TokenType.LPAREN)) {
			pr.register_advancement();
			advance();
			advanceNewLines(pr);
			Object ex = pr.register(expression());
			if(pr.error != null) return pr;
			if(currentToken.matches(TokenType.RPAREN)) {
				pr.register_advancement();
				advance();
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
		debug("Parser: multi-lines statements");
		
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
		
		debug("Parser: " + statements);
		
		return pr.success(new StatementsNode(statements));
	}
	
	private ParseResult statement() {
		debug("Parser: single statement");
		
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
		debug("Parser: List node");
		
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
		debug("Parser: for");
		
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

//				pr.register_advancement();
//				advance();
//				advanceNewLines(pr);
				
				if(!currentToken.matches(TokenType.RBRA))
					return pr.failure(new Error.SyntaxError("Expected '}' FORIN", currentToken.getSeq()));
				
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
		debug("Parser: while");
		
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
		debug("Parser: function");
		
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
		
		ArrayList<Token> temp_tokens = new ArrayList<Token>();
		
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
		debug("Parser: object");
		
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
		
		ArrayList<Token> args = new ArrayList<Token>();
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
			debug("Parser: Call "+atom);
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
		debug("Parser: expression");
		
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
		return pr;
	}
	
	public void debug(Object obj) {
		if(JIPL.debug)
			System.out.println("" + obj + " " + currentToken);
	}
	
}