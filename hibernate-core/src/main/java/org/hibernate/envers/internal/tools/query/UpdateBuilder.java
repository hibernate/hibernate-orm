/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools.query;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.internal.tools.MutableInteger;
import org.hibernate.query.Query;

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
		updates = new HashMap<>();
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
		final Map<String, Object> queryParamValues = new HashMap<>();

		build( querySb, queryParamValues );

		final Query query = session.createQuery( querySb.toString() );
		for ( Map.Entry<String, Object> paramValue : queryParamValues.entrySet() ) {
			query.setParameter( paramValue.getKey(), paramValue.getValue() );
		}

		return query;
	}
}
