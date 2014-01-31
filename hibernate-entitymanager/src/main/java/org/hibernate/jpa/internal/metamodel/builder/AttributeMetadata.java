/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.internal.metamodel.builder;

import java.lang.reflect.Member;
import javax.persistence.metamodel.Attribute;

import org.hibernate.jpa.internal.metamodel.AbstractManagedType;
import org.hibernate.metamodel.spi.binding.AttributeBinding;

/**
 * Basic contract for describing an attribute in a format needed while building the JPA metamodel.
 *
 * The "description" is described:<ol>
 *     <li>partially in terms of JPA ({@link #getPersistentAttributeType} and {@link #getOwnerType})</li>
 *     <li>partially in terms of Hibernate metamodel ({@link #getAttributeBinding})</li>
 *     <li>and partially just in terms of the java model itself ({@link #getMember} and {@link #getJavaType})</li>
 * </ol>
 *
 * @param <X> The attribute owner type
 * @param <Y> The attribute type.
 */
public interface AttributeMetadata<X,Y> {
	/**
	 * Retrieve the name of the attribute
	 *
	 * @return The attribute name
	 */
	public String getName();

	/**
	 * Retrieve the member defining the attribute
	 *
	 * @return The attribute member
	 */
	public Member getMember();

	/**
	 * Retrieve the attribute java type.
	 *
	 * @return The java type of the attribute.
	 */
	public Class<Y> getJavaType();

	/**
	 * Get the JPA attribute type classification for this attribute.
	 *
	 * @return The JPA attribute type classification
	 */
	public Attribute.PersistentAttributeType getPersistentAttributeType();

	/**
	 * Retrieve the attribute owner's metamodel information
	 *
	 * @return The metamodel information for the attribute owner
	 */
	public AbstractManagedType<X> getOwnerType();

	/**
	 * Retrieve the Hibernate property mapping related to this attribute.
	 *
	 * @return The Hibernate property mapping
	 */
	public AttributeBinding getAttributeBinding();

	/**
	 * Is the attribute plural (a collection)?
	 *
	 * @return True if it is plural, false otherwise.
	 */
	public boolean isPlural();
}
