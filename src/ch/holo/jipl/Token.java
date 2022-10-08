package ch.holo.jipl;

public class Token {
		
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
	
	public static enum TokenType {
		INT, FLOAT, STRING,
		PLUS, MINUS, MULT, DIV, POWER,
		DPLUS, DMINUS, PLUS_EQUAL, MINUS_EQUAL,
		EQUALS, DOUBLE_EQUALS, NOT_EQUALS, LESS, GREATER, LESS_EQUALS, GREATER_EQUALS, LPAREN, RPAREN, LSQUARE, RSQUARE, LBRA, RBRA,
		COLON, COMMAS, POINT, NLINE,
		KEYWORD, IDENTIFIER,
		END_OF_CODE;
	}
}