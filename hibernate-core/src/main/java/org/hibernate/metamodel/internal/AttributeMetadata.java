/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Member;

import org.hibernate.mapping.Property;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * Basic contract for describing an attribute.
 *
 * @param <X> The attribute owner type
 * @param <Y> The attribute type.
 */
public interface AttributeMetadata<X, Y> {
	/**
	 * Retrieve the name of the attribute
	 *
	 * @return The attribute name
	 */
	String getName();

	/**
	 * Retrieve the member defining the attribute
	 *
	 * @return The attribute member
	 */
	Member getMember();

	/**
	 * Retrieve the attribute java type.
	 *
	 * @return The java type of the attribute.
	 */
	Class<Y> getJavaType();

	/**
	 * Get the classification for this attribute
	 */
	AttributeClassification getAttributeClassification();

	/**
	 * Retrieve the attribute owner's metamodel information
	 *
	 * @return The metamodel information for the attribute owner
	 */
	ManagedDomainType<X> getOwnerType();

	/**
	 * Retrieve the Hibernate property mapping related to this attribute.
	 *
	 * @return The Hibernate property mapping
	 */
	Property getPropertyMapping();

	/**
	 * Is the attribute plural (a collection)?
	 *
	 * @return True if it is plural, false otherwise.
	 */
	boolean isPlural();
}
