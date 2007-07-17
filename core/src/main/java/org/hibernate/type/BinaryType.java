//$Id: BinaryType.java 10009 2006-06-10 03:24:05Z epbernard $
package org.hibernate.type;

/**
 * <tt>binary</tt>: A type that maps an SQL VARBINARY to a Java byte[].
 * @author Gavin King
 */
public class BinaryType extends AbstractBynaryType {

	protected Object toExternalFormat(byte[] bytes) {
		return bytes;
	}

	protected byte[] toInternalFormat(Object bytes) {
		return (byte[]) bytes;
	}

	public Class getReturnedClass() {
		return byte[].class;
	}

	public String getName() { return "binary"; }

}
