/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities;

import java.util.Objects;

/**
 * @author Chris Cranford
 * @author 6.0
 */
public class RevisionTimestampData extends PropertyData {

	private final String typeName;

	public RevisionTimestampData(String name, String beanName, String accessType, String typeName) {
		super( name, beanName, accessType );
		this.typeName = typeName;
	}

	public RevisionTimestampData(RevisionTimestampData old, String typeName) {
		this( old.getName(), old.getBeanName(), old.getAccessType(), typeName );
	}

	public String getTypeName() {
		return typeName;
	}

	public boolean isTimestampDate() {
		return "date".equals( typeName )
				|| "time".equals( typeName )
				|| "timestamp".equals( typeName );
	}

	public boolean isTimestampLocalDateTime() {
		return "LocalDateTime".equals( typeName );
	}

	public boolean isInstant() {
		return "instant".equals( typeName );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ( typeName != null ? typeName.hashCode() : 0 );
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
		return Objects.equals( typeName, that.typeName );
	}
}
