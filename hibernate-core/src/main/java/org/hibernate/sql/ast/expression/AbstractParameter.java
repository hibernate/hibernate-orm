/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.ast.select.SqlSelectable;
import org.hibernate.sql.convert.results.internal.ReturnScalarImpl;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sql.convert.spi.ParameterSpec;
import org.hibernate.sql.exec.results.process.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionReader;
import org.hibernate.sql.spi.ParameterBinder;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.Type;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractParameter
		implements ParameterSpec, ParameterBinder, Expression, SqlSelectable, Selectable {
	private static final Logger log = Logger.getLogger( AbstractParameter.class );

	private final Type inferredType;

	public AbstractParameter(Type inferredType) {
		this.inferredType = inferredType;
	}

	public Type getInferredType() {
		return inferredType;
	}

	@Override
	public Type getType() {
		return getInferredType();
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public Expression getSelectedExpression() {
		return this;
	}

	@Override
	public Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable) {
		return new ReturnScalarImpl(
				this,
				returnResolutionContext.resolveSqlSelection( this ),
				resultVariable,
				getType()
		);
	}

	@Override
	public ParameterBinder getParameterBinder() {
		return this;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( (BasicType) getType() );
	}

	protected int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			QueryParameterBinding valueBinding,
			SharedSessionContractImplementor session) throws SQLException {
		final Type bindType;
		final Object bindValue;

		if ( valueBinding == null ) {
			warnNoBinding();
			bindType = valueBinding.getBindType();
			bindValue = null;
		}
		else {
			if ( valueBinding.getBindType() == null ) {
				bindType = inferredType;
			}
			else {
				bindType = valueBinding.getBindType();
			}
			bindValue = valueBinding.getBindValue();
		}

		if ( bindType == null ) {
			unresolvedType();
		}
		assert bindType != null;
		if ( bindValue == null ) {
			warnNullBindValue();
		}

		bindType.nullSafeSet( statement, bindValue, startPosition, session );
		return bindType.getColumnSpan();
	}

	protected abstract void warnNoBinding();

	protected abstract void unresolvedType();

	protected abstract void warnNullBindValue();
}
