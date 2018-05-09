package org.hibernate.tool.hbm2x.pojo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

public class AnnotationBuilder {

	String annotation;
	LinkedHashMap<String, String[]> attributes = new LinkedHashMap<String, String[]>(); 
	
	public static AnnotationBuilder createAnnotation(String annotation) {
		return new AnnotationBuilder(annotation);
	}
	
	protected AnnotationBuilder(String annotation) {
		this.annotation = annotation;
	}

	public AnnotationBuilder addAttribute(String name, String[] values) {
		if(values!=null && values.length > 0) {				
			attributes.put(name, values);
		}
		return this;
	}

	public AnnotationBuilder addAttribute(String name, String value) {
		if(value!=null) {
			addAttribute(name, new String[] { value });
		}
		return this;
	}
	
	
	public AnnotationBuilder resetAnnotation(String annotationName) {
		this.annotation = annotationName;
		clearAttributes();
		return this;
	}
	
	private AnnotationBuilder clearAttributes() {
		attributes.clear();
		return this;
	}
	
	public String getResult() {
		StringBuffer b = new StringBuffer("@");
		b.append(annotation);
		if(attributes.isEmpty()) {
			return b.toString();
		} else {
			b.append("(");
			Iterator<Entry<String, String[]>> elements = attributes.entrySet().iterator();
			boolean addedBefore = false;
			while ( elements.hasNext() ) {
				Entry<String, String[]> element = elements.next();

				String[] s = element.getValue();
				if(s.length==0) {
					addedBefore = false;
					continue;
				} else {
					if(addedBefore) {
						b.append(", ");
					}
					String key = element.getKey();
					b.append(key).append("=");
					attributeToString( b, s );
					
					addedBefore=true;
				}
			}
			b.append( ")" );
		}
		return b.toString();
	}

	private void attributeToString(StringBuffer buffer, String[] values) {
		if(values.length>1) {
			buffer.append( "{" );
		}
		
		for (int i = 0; i < values.length; i++) {
			buffer.append(values[i]);
			if(i<values.length-1) {
				buffer.append(", ");
			}
		}		
		
		if(values.length>1) {
			buffer.append( "}" );
		}
		
	}

	public void addQuotedAttributes(String name, Iterator<?> iterator) {
		List<String> values = new ArrayList<String>();
		while ( iterator.hasNext() ) {
			values.add(quote( iterator.next().toString() ));
		}
		addAttribute(name, values.toArray( new String[values.size()] ));
	}

	public void addAttributes(String name, Iterator<?> iterator) {
		List<String> values = new ArrayList<String>();
		while ( iterator.hasNext() ) {
			String element = iterator.next().toString();
			values.add( element );
		}
		addAttribute(name, values.toArray( new String[values.size()] ));		
	}
	private String quote(String element) {
		return "\"" + element + "\"";
	}

	public AnnotationBuilder addQuotedAttribute(String name, String value) {
		if(value!=null) {
			addAttribute(name, quote(value));
		}
		return this;
	}
	

	public String toString() {
		return getResult();
	}

	public String getAttributeAsString(String name) {
		StringBuffer buffer = new StringBuffer();
		String[] object = (String[]) attributes.get( name );
		if(object==null) {
			return null;
		} else {
			attributeToString( buffer, object );
			return buffer.toString();
		}
	}

	
}

