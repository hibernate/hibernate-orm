//$Id: IdentifierType.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.type;

/**
 * A <tt>Type</tt> that may be used as an identifier.
 * @author Gavin King
 */
public interface IdentifierType extends Type {

	/**
	 * Convert the value from the mapping file to a Java object.
	 * @param xml the value of <tt>discriminator-value</tt> or <tt>unsaved-value</tt> attribute
	 * @return Object
	 * @throws Exception
	 */
	public Object stringToObject(String xml) throws Exception;

}






