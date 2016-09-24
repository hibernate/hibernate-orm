/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.old;

import org.hibernate.query.QueryParameter;
import org.hibernate.type.mapper.spi.Type;

/**
 * Models a named query parameter
 *
 * NOTE: Unfortunately we need to model named and positional parameters separately still until 6.0
 *
 * NOTE : Also, notice that this still treats JPA "positional" parameters as named.  This will change in
 * 6.0 as well after we remove support for legacy positional parameters (the JPA model is better there).
 *
 * @author Steve Ebersole
 */
public class QueryParameterNamedImpl<T> extends QueryParameterImpl<T> implements QueryParameter<T> {
	private final String name;
	private final int[] sourceLocations;
	private final boolean jpaStyle;

	public QueryParameterNamedImpl(String name, int[] sourceLocations, boolean jpaStyle, Type expectedType) {
		super( expectedType );
		this.name = name;
		this.sourceLocations = sourceLocations;
		this.jpaStyle = jpaStyle;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Integer getPosition() {
		return null;
	}

	public int[] getSourceLocations() {
		return sourceLocations;
	}

	@Override
	public boolean isJpaPositionalParameter() {
		return jpaStyle;
	}
}
