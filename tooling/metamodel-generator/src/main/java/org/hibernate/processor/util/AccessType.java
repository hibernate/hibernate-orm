/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.util;

/**
 * @author Hardy Ferentschik
 */
public enum AccessType {
	PROPERTY( jakarta.persistence.AccessType.PROPERTY ),
	FIELD( jakarta.persistence.AccessType.FIELD );

	private final jakarta.persistence.AccessType jpaAccessType;

	AccessType(jakarta.persistence.AccessType jpaAccessType) {
		this.jpaAccessType = jpaAccessType;
	}

	public jakarta.persistence.AccessType getJpaAccessType() {
		return jpaAccessType;
	}

	public static AccessType fromJpaAccessType(jakarta.persistence.AccessType jpaAccessType) {
		if ( jpaAccessType == jakarta.persistence.AccessType.FIELD ) {
			return FIELD;
		}
		if ( jpaAccessType == jakarta.persistence.AccessType.PROPERTY ) {
			return PROPERTY;
		}

		throw new IllegalArgumentException( "Unknown JPA AccessType - " + jpaAccessType.name() );
	}
}
