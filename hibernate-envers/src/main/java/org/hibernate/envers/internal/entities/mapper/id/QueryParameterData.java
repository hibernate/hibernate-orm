/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.internal.entities.mapper.id;

import org.hibernate.Query;
import org.hibernate.internal.util.compare.EqualsHelper;

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
		return EqualsHelper.equals( flatEntityPropertyName, that.flatEntityPropertyName );
	}

	@Override
	public int hashCode() {
		return (flatEntityPropertyName != null ? flatEntityPropertyName.hashCode() : 0);
	}
}
