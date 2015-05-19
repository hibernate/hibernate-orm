/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;


/**
 * Additional contract for a {@link Type} may be used for a discriminator.  THis contract is used to process
 * the string representation as presented in metadata, especially in <tt>XML</tt> files.
 *
 * @author Gavin King
 */
public interface IdentifierType<T> extends Type {

	/**
	 * Convert the value from the mapping file to a Java object.
	 *
	 * @param xml the value of <tt>discriminator-value</tt> or <tt>unsaved-value</tt> attribute
	 * @return The converted value of the string representation.
	 *
	 * @throws Exception Indicates a problem converting from the string
	 */
	public T stringToObject(String xml) throws Exception;

}
