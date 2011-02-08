/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
