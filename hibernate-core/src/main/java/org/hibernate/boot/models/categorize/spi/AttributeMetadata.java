/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;


import org.hibernate.models.spi.MemberDetails;

/**
 * Metadata about a persistent attribute
 *
 * @author Steve Ebersole
 */
public interface AttributeMetadata extends TableOwner {
	/**
	 * The attribute name
	 */
	String getName();

	/**
	 * The persistent nature of the attribute
	 */
	AttributeNature getNature();

	/**
	 * The backing member
	 */
	MemberDetails getMember();

	/**
	 * An enum defining the nature (categorization) of a persistent attribute.
	 *
	 * @see jakarta.persistence.metamodel.Attribute.PersistentAttributeType
	 */
	enum AttributeNature {
		BASIC,
		EMBEDDED,
		ANY,
		TO_ONE,
		ELEMENT_COLLECTION,
		MANY_TO_ANY,
		MANY_TO_MANY,
		ONE_TO_MANY
	}
}
