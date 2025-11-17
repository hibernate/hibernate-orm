/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import jakarta.persistence.criteria.CriteriaBuilder;

import java.util.Locale;

/**
 * Variations of the {@code trim()} function.
 *
 * @apiNote This is an SPI type allowing collaboration
 * between {@code org.hibernate.dialect} and
 * {@code org.hibernate.sqm}. It should never occur in
 * APIs visible to the application program.
 *
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
		else {
			return switch ( jpaTs ) {
				case BOTH -> BOTH;
				case LEADING -> LEADING;
				case TRAILING -> TRAILING;
			};
		}
	}

	public String toSqlText() {
		return name().toLowerCase( Locale.ROOT );
	}
}
