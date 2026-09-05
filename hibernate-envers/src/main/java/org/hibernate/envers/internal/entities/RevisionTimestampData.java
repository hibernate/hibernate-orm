/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

/**
 * @author Chris Cranford
 * @author 6.0
 */
public class RevisionTimestampData extends PropertyData {

	private final String typeName;
	private final Class<?> javaType;

	public RevisionTimestampData(
			String name,
			String beanName,
			String accessType,
			String typeName,
			Class<?> javaType) {
		super( name, beanName, accessType );
		this.typeName = typeName;
		this.javaType = javaType;
	}

	public RevisionTimestampData(RevisionTimestampData old, String typeName, Class<?> javaType) {
		this( old.getName(), old.getBeanName(), old.getAccessType(), typeName, javaType );
	}

	public String getTypeName() {
		return typeName;
	}

	public boolean isTimestampDate() {
		return Date.class.isAssignableFrom( javaType );
	}

	public boolean isTimestampLocalDateTime() {
		return LocalDateTime.class.equals( javaType );
	}

	public boolean isInstant() {
		return Instant.class.equals( javaType );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ( typeName != null ? typeName.hashCode() : 0 );
		result = 31 * result + ( javaType != null ? javaType.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		RevisionTimestampData that = (RevisionTimestampData) o;
		return Objects.equals( typeName, that.typeName )
				&& Objects.equals( javaType, that.javaType );
	}
}
