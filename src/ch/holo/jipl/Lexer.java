package ch.holo.jipl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import ch.holo.jipl.Token.TokenType;

public class Lexer {
	
	public static final String DIGITS = "0123456789";
	public static final String LETTERS = "azertyuiopqsdfghjklmwxcvbnAZERTYUIOPQSDFGHJKLMWXCVBN"+"àèéçôîÀÈÉÇêÊ";
	public static final String LEGAL_CHARS = LETTERS+DIGITS+"_$";
	
	public static final String[] KEYWORDS = {"var", "and", "or", "not", "true", "false", "null", "if", "elseif", "else", "for", "in", "to", "by", "while", "function", "return", "continue", "break", "new", "object", "extends", "this", "include"};
	
	public static ArrayList<Token> getTokens(String text, String file) {
		if(file != null)
			file = new File(file).getName();
		ArrayList<Token> list = new ArrayList<Token>();
		char[] chars = text.toCharArray();
		int line = 0;
		for(int i = 0; i < chars.length; i++) {
			char c = chars[i];
			
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
			else if(c == '*') i = _unaryComp(chars, i, TokenType.MULT_EQUAL, TokenType.MULT, TokenType.MULT, line, file, list);
			else if(c == '/') i = _unaryComp(chars, i, TokenType.DIV_EQUAL, TokenType.DIV, TokenType.DIV, line, file, list);
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
			for(int i = index+2; i < index+8 + 2; i++) {
				if("0123456789abcdef".contains(chars[i]+""))
					str+=chars[i];
				else {
					list.add(new Token(TokenType.INT, new Sequence(line, index, str.length()+2, file), ((int) Long.parseLong(str, 16))+""));
					return i-1;
				}
			}
			list.add(new Token(TokenType.INT, new Sequence(line, index, str.length()+2, file), ((int) Long.parseLong(str, 16))+""));
			return index+str.length()+1;
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
