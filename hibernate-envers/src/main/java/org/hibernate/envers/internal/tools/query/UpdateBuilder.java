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
package org.hibernate.envers.internal.tools.query;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.envers.internal.tools.MutableInteger;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class UpdateBuilder {
	private final String entityName;
	private final String alias;
	private final MutableInteger paramCounter;
	private final Parameters rootParameters;
	private final Map<String, Object> updates;

	public UpdateBuilder(String entityName, String alias) {
		this( entityName, alias, new MutableInteger() );
	}

	private UpdateBuilder(String entityName, String alias, MutableInteger paramCounter) {
		this.entityName = entityName;
		this.alias = alias;
		this.paramCounter = paramCounter;
		rootParameters = new Parameters( alias, "and", paramCounter );
		updates = new HashMap<String, Object>();
	}

	public Parameters getRootParameters() {
		return rootParameters;
	}

	public void updateValue(String propertyName, Object value) {
		updates.put( propertyName, value );
	}

	public void build(StringBuilder sb, Map<String, Object> updateParamValues) {
		sb.append( "update " ).append( entityName ).append( " " ).append( alias );
		sb.append( " set " );
		int i = 1;
		for ( String property : updates.keySet() ) {
			final String paramName = generateParameterName();
			sb.append( alias ).append( "." ).append( property ).append( " = " ).append( ":" ).append( paramName );
			updateParamValues.put( paramName, updates.get( property ) );
			if ( i < updates.size() ) {
				sb.append( ", " );
			}
			++i;
		}
		if ( !rootParameters.isEmpty() ) {
			sb.append( " where " );
			rootParameters.build( sb, updateParamValues );
		}
	}

	private String generateParameterName() {
		return "_u" + paramCounter.getAndIncrease();
	}

	public Query toQuery(Session session) {
		final StringBuilder querySb = new StringBuilder();
		final Map<String, Object> queryParamValues = new HashMap<String, Object>();

		build( querySb, queryParamValues );

		final Query query = session.createQuery( querySb.toString() );
		for ( Map.Entry<String, Object> paramValue : queryParamValues.entrySet() ) {
			query.setParameter( paramValue.getKey(), paramValue.getValue() );
		}

		return query;
	}
}
