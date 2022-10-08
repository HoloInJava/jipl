package ch.holo.jipl;

import java.io.Serializable;

public  class Sequence implements Serializable {
	private static final long serialVersionUID = 1L;
	private int line, offset, size;
	private String file;
	
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
		return "line "+line + "("+file+")";
	}
	
	public String apply(String text) { return text.substring(offset, offset+size); }
}