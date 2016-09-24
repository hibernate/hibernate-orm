/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.sql;

import org.hibernate.QueryException;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class NamedQueryParameterBinder extends AbstractParameterBinder {
	private static final Logger log = Logger.getLogger( NamedQueryParameterBinder.class );

	private final String name;

	public NamedQueryParameterBinder(String name) {
		this.name = name;
	}

	@Override
	protected QueryParameterBinding getBinding(QueryParameterBindings queryParameterBindings) {
		return queryParameterBindings.getBinding( name );
	}

	@Override
	protected void warnNoBinding() {
		log.debugf( "Query defined named parameter [%s], but no binding was found (setParameter not called)", name );
	}

	@Override
	protected void unresolvedType() {
		throw new QueryException( "Unable to determine Type for named parameter [" + name + "]" );
	}

	@Override
	protected void warnNullBindValue() {
		log.debugf( "Binding value for named parameter [:%s] was null", name );
	}
}
