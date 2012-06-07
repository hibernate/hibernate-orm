package org.hibernate.test.any;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * todo: describe ${NAME}
 *
 * @author Steve Ebersole
 */
public class ComplexPropertyValue implements PropertyValue {
	private Long id;
	private Map subProperties = new HashMap();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Map getSubProperties() {
		return subProperties;
	}

	public void setSubProperties(Map subProperties) {
		this.subProperties = subProperties;
	}

	public String asString() {
		return "complex[" + keyString() + "]";
	}

	private String keyString() {
		StringBuilder buff = new StringBuilder();
		Iterator itr = subProperties.keySet().iterator();
		while ( itr.hasNext() ) {
			buff.append( itr.next() );
			if ( itr.hasNext() ) {
				buff.append( ", " );
			}
		}
		return buff.toString();
	}
}
