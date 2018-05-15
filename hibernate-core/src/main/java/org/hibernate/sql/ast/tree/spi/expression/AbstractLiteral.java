/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * We classify literals different based on their source so that we can handle then differently
 * when rendering SQL.  This class offers convenience for those implementations
 * <p/>
 * Can function as a ParameterBinder for cases where we want to treat literals using bind parameters.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractLiteral
		implements JdbcParameterBinder, Expression, SqlExpressable, DomainResultProducer {
	private final Object value;
	private final SqlExpressableType type;
	private final Clause clause;

	public AbstractLiteral(Object value, SqlExpressableType type, Clause clause) {
		this.value = value;
		this.type = type;
		this.clause = clause;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	public boolean isInSelect() {
		return clause == Clause.SELECT;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		// todo (6.0) : for literals and parameters consider simply pushing these values directly into the "current JDBC values" array
		//		rather than reading them (the same value over and over) from the ResultSet.
		//
		//		see `org.hibernate.sql.ast.tree.spi.expression.AbstractParameter.createSqlSelection`

		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				getType()
		);
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {

		// todo (6.0) : consider just returning the literal value back directly

		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						this,
						getType().getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				getType()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			ExecutionContext executionContext) throws SQLException {
		getType().getJdbcValueBinder().bind( statement, startPosition, value, executionContext );
		return 1;
	}
}
