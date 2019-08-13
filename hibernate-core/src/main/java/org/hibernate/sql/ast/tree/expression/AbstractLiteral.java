/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.mapping.spi.ValueMapping;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.ValueMappingExpressable;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * We classify literals different based on their source so that we can handle then differently
 * when rendering SQL.  This class offers convenience for those implementations
 * <p/>
 * Can function as a ParameterBinder for cases where we want to treat literals using bind parameters.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractLiteral
		implements JdbcParameterBinder, Expression, ValueMappingExpressable, DomainResultProducer {
	private final Object value;
	private final ValueMappingExpressable type;
	private final Clause clause;

	public AbstractLiteral(Object value, ValueMappingExpressable type, Clause clause) {
		this.value = value;
		this.type = type;
		this.clause = clause;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public ValueMapping getExpressableValueMapping() {
		return type.getExpressableValueMapping();
	}

	public boolean isInSelect() {
		return clause == Clause.SELECT;
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) throws SQLException {
		throw new NotYetImplementedFor6Exception( getClass() );
//		getType().getJdbcValueBinder().bind( statement, startPosition, value, executionContext );
	}

	@Override
	public ValueMappingExpressable getExpressionType() {
		return type;
	}
}
