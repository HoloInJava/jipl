package ch.holo.jipl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import ch.holo.jipl.Interpreter.Function;
import ch.holo.jipl.Interpreter.List;
import ch.holo.jipl.Interpreter.Number;
import ch.holo.jipl.Interpreter.ObjectValue;
import ch.holo.jipl.Interpreter.StringValue;

public class Context implements Serializable {
		
		private static final long serialVersionUID = 1L;
		public String displayName, file;
		public Context parent = null;

		public HashMap<String, Object> symbols;
		public HashMap<String, Consumer<Object>> setterTracks = new HashMap<>();
		public HashMap<String, Callable<Object>> getterTracks = new HashMap<>();
		
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
			} else if(parent != null && parent != this && parent.parent != this)
				return parent.get(name);
			
			return Number.NULL;
		}
		
		public Context getSource(String name) {
			if(symbols.containsKey(name)) return this;
			if(parent != null && parent != this) return parent.getSource(name);
			return this;
		}
		
		public Context getSource(String name, Context def) {
			if(symbols.containsKey(name)) return this;
			if(parent != null) return parent.getSource(name);
			return def;
		}
		
		public boolean hasAsParent(Context con) {
			return parent != null && (parent != con || parent.hasAsParent(con));
		}
		
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