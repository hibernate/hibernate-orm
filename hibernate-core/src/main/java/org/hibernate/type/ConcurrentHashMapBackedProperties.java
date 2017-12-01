package org.hibernate.type;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ConcurrentHashMapBackedProperties extends Properties {

	/**
	 * 
	 */
	private static final long serialVersionUID = 23632462472472L;

	private ConcurrentHashMap<String,String> properties = new ConcurrentHashMap<String,String>();
	

	public ConcurrentHashMapBackedProperties(Properties properties){
		this.properties.putAll((Map)properties);
		
	}

	public ConcurrentHashMapBackedProperties(){
		
		super();
	}

	@Override
	public Object setProperty(String key, String value) {
		
		return properties.put(key, value);
	}

	@Override
	public void load(Reader reader) throws IOException {
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public void load(InputStream inStream) throws IOException {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public void save(OutputStream out, String comments) {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public void store(Writer writer, String comments) throws IOException {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public void store(OutputStream out, String comments) throws IOException {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public void storeToXML(OutputStream os, String comment) throws IOException {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public String getProperty(String key) {
		
		return (String) this.properties.get(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		 String val = getProperty(key);
	        return (val == null) ? defaultValue : val;
	}

	@Override
	public Enumeration<?> propertyNames() {
		
		return properties.keys();
	}

	@Override
	public Set<String> stringPropertyNames() {
		ConcurrentHashMap<String,String> strProps = new ConcurrentHashMap<String, String>();
		 for (Enumeration<?> e = keys() ; e.hasMoreElements() ;) {
	            Object k = e.nextElement();
	            Object v = get(k);
	            if (k instanceof String && v instanceof String) {
	                this.properties.put((String) k, (String) v);
	            }
	        }
		return strProps.keySet();
	}

	@Override
	public void list(PrintStream out) {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public void list(PrintWriter out) {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  int size() {
		
		return properties.size();
	}

	@Override
	public  boolean isEmpty() {
		
		return properties.isEmpty();
	}

	@Override
	public  Enumeration<Object> keys() {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  Enumeration<Object> elements() {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  boolean contains(Object value) {
		
		return properties.contains(value);
	}

	@Override
	public boolean containsValue(Object value) {
		
		return properties.containsValue(value);
	}

	@Override
	public  boolean containsKey(Object key) {
		
		return properties.containsKey(key);
	}

	@Override
	public  Object get(Object key) {
		
		return properties.get(key);
	}

	@Override
	protected void rehash() {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  Object put(Object key, Object value) {
		
		return properties.put((String)key, (String)value);
	}

	@Override
	public  Object remove(Object key) {
		
		return properties.remove(key);
	}

	@Override
	public  void putAll(Map<? extends Object, ? extends Object> t) {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  void clear() {
		
		properties.clear();
	}

	@Override
	public  Object clone() {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  String toString() {
		
		return properties.toString();
	}

	@Override
	public Set<Object> keySet() {
		

		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public Set<java.util.Map.Entry<Object, Object>> entrySet() {
		

		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public Collection<Object> values() {
		

		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  boolean equals(Object o) {
		
	return properties.equals(o);
	}

	@Override
	public  int hashCode() {
		
		return properties.hashCode();
	}

	@Override
	public  Object getOrDefault(Object key, Object defaultValue) {
		
	
		return properties.getOrDefault((String)key, (String)defaultValue);
	}
	@Override
	public  void forEach(BiConsumer<? super Object, ? super Object> action) {
		
		properties.forEach(action);
	}

	@Override
	public  void replaceAll(BiFunction<? super Object, ? super Object, ? extends Object> function) {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  Object putIfAbsent(Object key, Object value) {
		
		return properties.putIfAbsent((String)key, (String)value);
	}

	@Override
	public  boolean remove(Object key, Object value) {
		
		return properties.remove(key, value);
	}

	@Override
	public  boolean replace(Object key, Object oldValue, Object newValue) {
		
		return properties.replace((String)key, (String)oldValue, (String)newValue);
	}

	@Override
	public  Object replace(Object key, Object value) {
		
		return properties.replace((String)key, (String)value);
	}

	@Override
	public  Object computeIfAbsent(Object key, Function<? super Object, ? extends Object> mappingFunction) {
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  Object computeIfPresent(Object key,
			BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  Object compute(Object key,
			BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}

	@Override
	public  Object merge(Object key, Object value,
			BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
		
		throw new UnsupportedOperationException("Error: This method is not supported");
	}
	
	


}
