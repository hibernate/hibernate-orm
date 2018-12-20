/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain;

import java.util.EnumSet;

/**
 * Identifies specific mapping features used by a {@link DomainModelDescriptor}.
 *
 * The intent is to help categorize which models use specific mapping features
 * to help facilitate testing various outcomes based on those features.
 *
 * For example, when writing a test that depends on JPA's {@link javax.persistence.AttributeConverter},
 * we could just see which DomainModel reports using {@link #CONVERTER} and re-use that
 * model.
 *
 * @author Steve Ebersole
 */
public enum MappingFeature {
	CONVERTER,
	ENUMERATED,
	DYNAMIC_MODEL,

	DISCRIMINATOR_INHERIT,
	JOINED_INHERIT,
	UNION_INHERIT,

	SECONDARY_TABLE,

	AGG_COMP_ID,
	NON_AGG_COMP_ID,
	ID_CLASS,

	EMBEDDABLE,
	MANY_ONE,
	ONE_ONE,
	ONE_MANY,
	MANY_MANY,
	ANY,
	MANY_ANY,

	COLLECTION_TABLE,
	JOIN_TABLE,
	JOIN_COLUMN,

	;

	public static EnumSet<MappingFeature> all() {
		return EnumSet.allOf( MappingFeature.class );
	}
}
