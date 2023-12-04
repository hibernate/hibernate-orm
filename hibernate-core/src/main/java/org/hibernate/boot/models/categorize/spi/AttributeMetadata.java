/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
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
	 */
	enum AttributeNature {
		BASIC,
		EMBEDDED,
		ANY,
		TO_ONE,
		PLURAL
	}
}
