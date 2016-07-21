/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.sqm.convert.spi.ParameterBinder;
import org.hibernate.type.spi.Type;

/*
 * We classify literals different based on their source so that we can handle then differently
 * when rendering SQL.  This class offers convenience for those implementations
 * <p/>
 * Can function as a ParameterBinder for cases where we want to treat literals using bind parameters.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractLiteral extends SelfReadingExpressionSupport implements ParameterBinder {
	private final Object value;
	private final Type ormType;

	public AbstractLiteral(Object value, Type ormType) {
		this.value = value;
		this.ormType = ormType;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public Type getType() {
		return ormType;
	}

	@Override
	public int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			QueryParameterBindings queryParameterBindings,
			SessionImplementor session) throws SQLException {
		getType().nullSafeSet( statement, getValue(), startPosition, session );
		return getType().getColumnSpan();
	}
}
