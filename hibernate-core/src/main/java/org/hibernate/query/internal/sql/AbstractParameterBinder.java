/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.spi.ParameterBinder;
import org.hibernate.type.spi.Type;

/**
 * Abstract ParameterBinder implementation for QueryParameter binding.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractParameterBinder implements ParameterBinder {
	@Override
	public int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session) throws SQLException {
		final QueryParameterBinding binding = getBinding( queryParameterBindings );
		return bindParameterValue(  statement, startPosition, binding, session );
	}

	protected abstract QueryParameterBinding getBinding(QueryParameterBindings queryParameterBindings);

	protected int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			QueryParameterBinding valueBinding,
			SharedSessionContractImplementor session) throws SQLException {
		final Type bindType;
		final Object bindValue;

		if ( valueBinding == null ) {
			warnNoBinding();
			bindType = null;
			bindValue = null;
			return 1;
		}
		else {
			if ( valueBinding.getBindType() == null ) {
				bindType = null;
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
