/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.exec.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.type.spi.Type;

import org.jboss.logging.Logger;

/**
 * Represents a named parameter coming from the query.
 *
 * @author Steve Ebersole
 */
public class NamedParameter extends AbstractParameter {
	private static final Logger log = Logger.getLogger( NamedParameter.class );

	private final String name;

	public NamedParameter(String name, Type inferredType) {
		super( inferredType );
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session) throws SQLException {
		final QueryParameterBinding binding = queryParameterBindings.getBinding( name );
		return bindParameterValue( statement, startPosition, binding, session );
	}

	@Override
	protected void warnNoBinding() {
		log.debugf( "Query defined named parameter [%s], but no binding was found (setParameter not called)", getName() );
	}

	@Override
	protected void unresolvedType() {
		throw new QueryException( "Unable to determine Type for named parameter [" + getName() + "]" );
	}

	@Override
	protected void warnNullBindValue() {
		log.debugf( "Binding value for named parameter [%s] was null", getName() );
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
		walker.visitNamedParameter( this );
	}
}
