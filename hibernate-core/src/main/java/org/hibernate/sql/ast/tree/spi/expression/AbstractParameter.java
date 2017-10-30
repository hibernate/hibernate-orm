/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.ParameterBindingContext;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.QueryResultProducer;
import org.hibernate.sql.results.spi.SqlSelection;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractParameter implements GenericParameter, QueryResultProducer {
	private static final Logger log = Logger.getLogger( AbstractParameter.class );

	private final AllowableParameterType inferredType;

	public AbstractParameter(AllowableParameterType inferredType) {
		this.inferredType = inferredType;
	}

	public AllowableParameterType getInferredType() {
		return inferredType;
	}

	@Override
	public AllowableParameterType getType() {
		return getInferredType();
	}

	@Override
	public JdbcParameterBinder getParameterBinder() {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			ParameterBindingContext bindingContext) throws SQLException {
		final AllowableParameterType bindType;
		final Object bindValue;

		final QueryParameterBinding valueBinding = resolveBinding( bindingContext );
		if ( valueBinding == null ) {
			warnNoBinding();
			bindType = null;
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

		bindType.getValueBinder().bind( statement, bindValue, startPosition, bindingContext.getSession() );

		return bindType.getNumberOfJdbcParametersToBind();
	}

	protected abstract void warnNoBinding();

	protected abstract void unresolvedType();

	protected abstract void warnNullBindValue();



	// todo (6.0) : both of the methods below are another manifestation of only really allowing basic (single column) valued parameters


	@Override
	public QueryResult createQueryResult(
			String resultVariable, QueryResultCreationContext creationContext) {
		return new ScalarQueryResultImpl(
				resultVariable,
				creationContext.getSqlSelectionResolver().resolveSqlSelection( this ),
				(BasicValuedExpressableType) getType()
		);
	}

	@Override
	public SqlSelection createSqlSelection(int jdbcPosition) {
		// todo (6.0) : we should really just access the parameter bind value here rather than reading from ResultSet
		//		should be more performant - double so if we can resolve the bind here
		//		and encode it into the SqlSelectionReader
		//
		//		see `org.hibernate.sql.ast.tree.spi.expression.AbstractLiteral.createSqlSelection`

		return new SqlSelectionImpl(
				jdbcPosition,
				this,
				( (BasicValuedExpressableType) getType() ).getBasicType().getSqlSelectionReader()
		);
	}
}
