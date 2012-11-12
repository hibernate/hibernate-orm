/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
