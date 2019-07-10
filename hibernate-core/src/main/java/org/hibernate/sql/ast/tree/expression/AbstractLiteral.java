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
import org.hibernate.persister.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
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
			JavaTypeDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		// todo (6.0) : for literals and parameters consider simply pushing these values directly into the "current JDBC values" array
		//		rather than reading them (the same value over and over) from the ResultSet.
		//
		//		see `org.hibernate.sql.ast.tree.expression.AbstractParameter.createSqlSelection`

		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				getType()
		);
	}

	@Override
	public DomainResult createDomainResult(
			int valuesArrayPosition,
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
	public SqlExpressableType getType() {
		return type;
	}
}
