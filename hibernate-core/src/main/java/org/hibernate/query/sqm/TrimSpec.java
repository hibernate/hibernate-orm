/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
