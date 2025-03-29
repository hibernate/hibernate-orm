/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.util.Objects;

import org.hibernate.query.Query;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class QueryParameterData {
	private String flatEntityPropertyName;
	private Object value;

	public QueryParameterData(String flatEntityPropertyName, Object value) {
		this.flatEntityPropertyName = flatEntityPropertyName;
		this.value = value;
	}

	public String getProperty(String prefix) {
		if ( prefix != null ) {
			return prefix + "." + flatEntityPropertyName;
		}
		else {
			return flatEntityPropertyName;
		}
	}

	public Object getValue() {
		return value;
	}

	public void setParameterValue(Query query) {
		query.setParameter( flatEntityPropertyName, value );
	}

	public String getQueryParameterName() {
		return flatEntityPropertyName;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof QueryParameterData) ) {
			return false;
		}

		final QueryParameterData that = (QueryParameterData) o;
		return Objects.equals( flatEntityPropertyName, that.flatEntityPropertyName );
	}

	@Override
	public int hashCode() {
		return (flatEntityPropertyName != null ? flatEntityPropertyName.hashCode() : 0);
	}
}
