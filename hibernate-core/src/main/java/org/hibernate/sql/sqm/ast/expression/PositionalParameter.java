/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class PositionalParameter extends AbstractParameter {
	private static final Logger log = Logger.getLogger( PositionalParameter.class );

	private final int position;

	public PositionalParameter(int position, Type inferredType) {
		super( inferredType );
		this.position = position;
	}

	public int getPosition() {
		return position;
	}

	@Override
	public int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			QueryParameterBindings queryParameterBindings,
			SessionImplementor session) throws SQLException {
		final QueryParameterBinding binding = queryParameterBindings.getBinding( position );
		return bindParameterValue(  statement, startPosition, binding, session );
	}

	@Override
	protected void warnNoBinding() {
		log.debugf( "Query defined positional parameter [%s], but no binding was found (setParameter not called)", getPosition() );
	}

	@Override
	protected void unresolvedType() {
		throw new QueryException( "Unable to determine Type for positional parameter [" + getPosition() + "]" );
	}

	@Override
	protected void warnNullBindValue() {
		log.debugf( "Binding value for positional parameter [:%s] was null", getPosition() );
	}

	@Override
	public void accept(SqlTreeWalker sqlTreeWalker) {
		sqlTreeWalker.visitPositionalParameter( this );
	}
}
