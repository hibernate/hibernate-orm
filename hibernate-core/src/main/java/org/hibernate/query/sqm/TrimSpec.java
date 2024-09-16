/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.hibernate.AssertionFailure;

import java.util.Locale;

/**
 * @author Steve Ebersole
 */
public enum TrimSpec {
	LEADING,
	TRAILING,
	BOTH;

	public static TrimSpec fromCriteriaTrimSpec(CriteriaBuilder.Trimspec jpaTs) {
		if ( jpaTs == null ) {
			return null;
		}

		switch ( jpaTs ) {
			case BOTH:
				return BOTH;
			case LEADING:
				return LEADING;
			case TRAILING:
				return TRAILING;
			default:
				throw new AssertionFailure( "Unrecognized JPA TrimSpec" );

		}
	}

	public String toSqlText() {
		return name().toLowerCase( Locale.ROOT );
	}
}
