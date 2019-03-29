/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.sqm.spi.SqmInterpretation;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.consume.internal.SqmConsumeHelper.generateJdbcParamsXref;

/**
 * @author Steve Ebersole
 */
public class QueryHelper {
	private QueryHelper() {
		// disallow direct instantiation
	}

	public static JdbcParameterBindings buildJdbcParameterBindings(
			SqmStatement sqmStatement,
			SqmInterpretation sqmInterpretation,
			ExecutionContext executionContext) {
		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqmStatement );
		final Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref =
				generateJdbcParamsXref( domainParameterXref, sqmInterpretation );
		return createJdbcParameterBindings(
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				executionContext.getSession()
		);
	}

	public static JdbcParameterBindings buildJdbcParameterBindings(
			SqmStatement sqmStatement,
			Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref,
			ExecutionContext executionContext) {
		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqmStatement );
		return createJdbcParameterBindings(
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				executionContext.getSession()
		);
	}

	public static JdbcParameterBindings buildJdbcParameterBindings(
			DomainParameterXref domainParameterXref,
			Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref,
			ExecutionContext executionContext) {
		return createJdbcParameterBindings(
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				executionContext.getSession()
		);
	}

	public static JdbcParameterBindings createJdbcParameterBindings(
			QueryParameterBindings<QueryParameterBinding<?>> domainParamBindings,
			DomainParameterXref domainParameterXref,
			Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamXref,
			SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();

		for ( Map.Entry<QueryParameterImplementor<?>, List<SqmParameter>> entry :
				domainParameterXref.getSqmParamByQueryParam().entrySet() ) {
			final QueryParameterImplementor<?> queryParam = entry.getKey();
			final List<SqmParameter> sqmParameters = entry.getValue();

			final QueryParameterBinding<?> domainParamBinding = domainParamBindings.getBinding( queryParam );
			final AllowableParameterType<?> parameterType = determineParameterType( domainParamBinding, queryParam, session );

			final Map<SqmParameter, List<JdbcParameter>> jdbcParamMap = jdbcParamXref.get( queryParam );
			for ( SqmParameter sqmParameter : sqmParameters ) {
				final List<JdbcParameter> jdbcParams = jdbcParamMap.get( sqmParameter );

				if ( ! domainParamBinding.isBound() ) {
					parameterType.visitJdbcTypes(
							new Consumer<SqlExpressableType>() {
								int position = 0;

								@Override
								public void accept(SqlExpressableType jdbcType) {
									final JdbcParameter jdbcParameter = jdbcParams.get( position++ );
									jdbcParameterBindings.addBinding(
											jdbcParameter,
											new JdbcParameterBinding() {
												@Override
												public SqlExpressableType getBindType() {
													return jdbcParameter.getType();
												}

												@Override
												public Object getBindValue() {
													return null;
												}
											}
									);
								}
							},
							Clause.IRRELEVANT,
							session.getFactory().getTypeConfiguration()
					);
				}
				else if ( domainParamBinding.isMultiValued() ) {
					final Collection<?> bindValues = domainParamBinding.getBindValues();
					final Iterator<?> valueItr = bindValues.iterator();

					// the original SqmParameter is the one we are processing.. create a binding for it..
					createValueBindings( jdbcParameterBindings, parameterType, jdbcParams, valueItr.next(), session );

					// an then one for each of the expansions
					final List<SqmParameter> expansions = domainParameterXref.getExpansions( sqmParameter );
					assert expansions.size() == bindValues.size() - 1;
					int expansionPosition = 0;
					while ( valueItr.hasNext() ) {
						final SqmParameter expansionSqmParam = expansions.get( expansionPosition++ );
						final List<JdbcParameter> expansionJdbcParams = jdbcParamMap.get( expansionSqmParam );
						createValueBindings( jdbcParameterBindings, parameterType, expansionJdbcParams, valueItr.next(), session );
					}
				}
				else {
					final Object bindValue = domainParamBinding.getBindValue();
					createValueBindings( jdbcParameterBindings, parameterType, jdbcParams, bindValue, session );
				}
			}
		}

		return jdbcParameterBindings;
	}

	private static void createValueBindings(
			JdbcParameterBindings jdbcParameterBindings,
			AllowableParameterType<?> parameterType,
			List<JdbcParameter> jdbcParams,
			Object bindValue,
			SharedSessionContractImplementor session) {
		parameterType.dehydrate(
				parameterType.unresolve( bindValue, session ),
				new ExpressableType.JdbcValueCollector() {
					private int position = 0;

					@Override
					public void collect(Object jdbcValue, SqlExpressableType type, Column boundColumn) {
						final JdbcParameter jdbcParameter = jdbcParams.get( position );
						jdbcParameterBindings.addBinding(
								jdbcParameter,
								new JdbcParameterBinding() {
									@Override
									public SqlExpressableType getBindType() {
										return jdbcParameter.getType();
									}

									@Override
									public Object getBindValue() {
										return jdbcValue;
									}
								}
						);
						position++;
					}
				},
				Clause.IRRELEVANT,
				session
		);
	}

	private static AllowableParameterType determineParameterType(
			QueryParameterBinding<?> binding,
			QueryParameterImplementor<?> parameter,
			SharedSessionContractImplementor session) {
		if ( binding.getBindType() != null ) {
			return binding.getBindType();
		}

		if ( parameter.getHibernateType() != null ) {
			return parameter.getHibernateType();
		}

		final TypeConfiguration typeConfiguration = session.getFactory().getTypeConfiguration();

		// assume we have (or can create) a mapping for the parameter's Java type
		return typeConfiguration.standardExpressableTypeForJavaType( parameter.getParameterType() );
	}
}
