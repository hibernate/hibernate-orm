/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.Bindable;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.AllowableParameterType;
import org.hibernate.query.internal.BindingTypeHelper;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.query.sql.spi.ParameterOccurrence;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.type.BasicTypeReference;

/**
 * Access to all of the externalized JDBC parameter bindings
 *
 * @apiNote "Externalized" because some JDBC parameter values are
 * intrinsically part of the parameter itself and we do not need to
 * locate a JdbcParameterBinding.  E.g., consider a
 * {@link org.hibernate.sql.ast.tree.expression.LiteralAsParameter}
 * which actually encapsulates the actually literal value inside
 * itself - to create the binder and actually perform the binding
 * is only dependent on the LiteralParameter
 *
 * @author Steve Ebersole
 */
public interface JdbcParameterBindings {
	void addBinding(JdbcParameter parameter, JdbcParameterBinding binding);

	Collection<JdbcParameterBinding> getBindings();

	JdbcParameterBinding getBinding(JdbcParameter parameter);

	void visitBindings(BiConsumer<JdbcParameter, JdbcParameterBinding> action);

	JdbcParameterBindings NO_BINDINGS = new JdbcParameterBindings() {
		@Override
		public void addBinding(JdbcParameter parameter, JdbcParameterBinding binding) {
		}

		@Override
		public Collection<JdbcParameterBinding> getBindings() {
			return Collections.emptyList();
		}

		@Override
		public JdbcParameterBinding getBinding(JdbcParameter parameter) {
			return null;
		}

		@Override
		public void visitBindings(BiConsumer<JdbcParameter, JdbcParameterBinding> action) {
		}
	};

	default int registerParametersForEachJdbcValue(
			Object value,
			Clause clause,
			Bindable bindable,
			List<JdbcParameter> jdbcParameters,
			SharedSessionContractImplementor session) {
		return registerParametersForEachJdbcValue( value, clause, 0, bindable, jdbcParameters, session );
	}

	default int registerParametersForEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			Bindable bindable,
			List<JdbcParameter> jdbcParameters,
			SharedSessionContractImplementor session) {
		return bindable.forEachJdbcValue(
				value,
				clause,
				offset,
				(selectionIndex, jdbcValue, type) -> {
					addBinding(
						jdbcParameters.get( selectionIndex ),
						new JdbcParameterBindingImpl(
								BindingTypeHelper.INSTANCE.resolveBindType(
										jdbcValue,
										type,
										session.getFactory().getTypeConfiguration()
								),
								jdbcValue
						)
					);
				}
				,
				session
		);
	}

	default void registerNativeQueryParameters(
			QueryParameterBindings queryParameterBindings,
			List<ParameterOccurrence> parameterOccurrences,
			List<JdbcParameterBinder> jdbcParameterBinders,
			SessionFactoryImplementor factory) {
		final Dialect dialect = factory.getServiceRegistry().getService( JdbcServices.class ).getJdbcEnvironment().getDialect();
		final boolean paddingEnabled = factory.getSessionFactoryOptions().inClauseParameterPaddingEnabled();
		final int inExprLimit = dialect.getInExpressionCountLimit();

		for ( ParameterOccurrence occurrence : parameterOccurrences ) {
			final QueryParameterImplementor<?> param = occurrence.getParameter();
			final QueryParameterBinding<?> binding = queryParameterBindings.getBinding( param );

			final JdbcMapping jdbcMapping;

			final AllowableParameterType<?> type = determineParamType( param, binding );
			if ( type == null ) {
				jdbcMapping = factory.getTypeConfiguration().getBasicTypeForJavaType( Object.class );
			}
			else if ( type instanceof BasicTypeReference ) {
				jdbcMapping = factory.getTypeConfiguration()
						.getBasicTypeRegistry()
						.resolve( ( (BasicTypeReference<?>) type ) );
			}
			else if ( type instanceof BasicValuedMapping ) {
				jdbcMapping = ( (BasicValuedMapping) type ).getJdbcMapping();
			}
			else {
				throw new IllegalArgumentException( "Could not resolve NativeQuery parameter type : `" + param + "`");
			}

			if ( binding.isMultiValued() ) {
				final Collection<?> bindValues = binding.getBindValues();
				final int bindValueCount = bindValues.size();
				Object lastBindValue = null;
				for ( Object bindValue : bindValues ) {
					final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );
					jdbcParameterBinders.add( jdbcParameter );
					addBinding( jdbcParameter, new JdbcParameterBindingImpl( jdbcMapping, bindValue ) );
					lastBindValue = bindValue;
				}
				final int bindValueMaxCount = NativeQueryImpl.determineBindValueMaxCount(
						paddingEnabled,
						inExprLimit,
						bindValueCount
				);
				if ( bindValueMaxCount != bindValueCount ) {
					for ( int i = bindValueCount; i < bindValueMaxCount; i++ ) {
						final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );
						jdbcParameterBinders.add( jdbcParameter );
						addBinding( jdbcParameter, new JdbcParameterBindingImpl( jdbcMapping, lastBindValue ) );
					}
				}
			}
			else {
				final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );
				jdbcParameterBinders.add( jdbcParameter );
				addBinding(
						jdbcParameter,
						new JdbcParameterBindingImpl( jdbcMapping, binding.getBindValue() )
				);
			}
		}
	}

	private AllowableParameterType<?> determineParamType(QueryParameterImplementor<?> param, QueryParameterBinding<?> binding) {
		AllowableParameterType<?> type = binding.getBindType();
		if ( type == null ) {
			type = param.getHibernateType();
		}
		return type;
	}
}
