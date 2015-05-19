/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.usertype;

/**
 * A custom type that may function as an identifier or discriminator type
 * 
 * @author Gavin King
 */
public interface EnhancedUserType extends UserType {
	/**
	 * Return an SQL literal representation of the value
	 */
	public String objectToSQLString(Object value);
	
	/**
	 * Return a string representation of this value, as it should appear in an XML document
	 *
	 * @deprecated To be removed in 5.  Implement {@link org.hibernate.type.StringRepresentableType#toString(Object)}
	 * instead.  See <a href="https://hibernate.onjira.com/browse/HHH-7776">HHH-7776</a> for details
	 */
	@Deprecated
	public String toXMLString(Object value);

	/**
	 * Parse a string representation of this value, as it appears in an XML document
	 *
	 * @deprecated To be removed in 5.  Implement
	 * {@link org.hibernate.type.StringRepresentableType#fromStringValue(String)} instead.
	 * See <a href="https://hibernate.onjira.com/browse/HHH-7776">HHH-7776</a> for details
	 */
	@Deprecated
	public Object fromXMLString(String xmlValue);
}
