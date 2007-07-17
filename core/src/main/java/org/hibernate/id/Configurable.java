//$Id: Configurable.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.Type;

/**
 * An <tt>IdentifierGenerator</tt> that supports "configuration".
 *
 * @see IdentifierGenerator
 * @author Gavin King
 */
public interface Configurable {

	/**
	 * Configure this instance, given the value of parameters
	 * specified by the user as <tt>&lt;param&gt;</tt> elements.
	 * This method is called just once, following instantiation.
	 *
	 * @param params param values, keyed by parameter name
	 */
	public void configure(Type type, Properties params, Dialect d) throws MappingException;

}
