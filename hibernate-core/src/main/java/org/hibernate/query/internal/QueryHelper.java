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
import java.util.function.Supplier;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.Navigable;
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

	public static ExpressableType<?> highestPrecedenceType(ExpressableType<?>... types) {
		if ( types == null || types.length == 0 ) {
			return null;
		}

		if ( types.length == 1 ) {
			return types[0];
		}

		//noinspection unchecked
		ExpressableType highest = highestPrecedenceType2( (ExpressableType) types[0], types[1] );
		for ( int i = 2; i < types.length; i++ ) {
			//noinspection unchecked
			highest = highestPrecedenceType2( highest, types[i] );
		}
		return highest;
	}

	public static <X> ExpressableType<X> highestPrecedenceType2(ExpressableType<X> type1, ExpressableType<X> type2) {
		if ( type1 == null && type2 == null ) {
			return null;
		}
		else if ( type1 == null ) {
			return type2;
		}
		else if ( type2 == null ) {
			return type1;
		}

		if ( type1 instanceof Navigable ) {
			return type1;
		}

		if ( type2 instanceof Navigable ) {
			return type2;
		}

		// any other precedence rules?

		return type1;
	}

	/**
	 * @see TypeConfiguration#resolveArithmeticType
	 */
	public static <X> ExpressableType<X> highestPrecedenceType(Supplier<ExpressableType<X>>... typeSuppliers) {
		if ( typeSuppliers == null || typeSuppliers.length == 0 ) {
			return null;
		}

		if ( typeSuppliers.length == 1 ) {
			return typeSuppliers[0].get();
		}

		ExpressableType<X> highest = highestPrecedenceType2( typeSuppliers[0].get(), typeSuppliers[1].get() );
		for ( int i = 2; i < typeSuppliers.length; i++ ) {
			highest = highestPrecedenceType2( highest, typeSuppliers[i].get() );
		}
		return highest;
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

			final Map<SqmParameter, List<JdbcParameter>> jdbcParamMap = jdbcParamXref.get( queryParam );
			for ( SqmParameter sqmParameter : sqmParameters ) {
				final List<JdbcParameter> jdbcParams = jdbcParamMap.get( sqmParameter );
				final AllowableParameterType<?> parameterType = determineParameterType(
						domainParamBinding,
						queryParam,
						session.getFactory().getTypeConfiguration()
				);

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

	public static AllowableParameterType determineParameterType(
			QueryParameterBinding<?> binding,
			QueryParameterImplementor<?> parameter,
			TypeConfiguration typeConfiguration) {
		if ( binding.getBindType() != null ) {
			return binding.getBindType();
		}

		if ( parameter.getHibernateType() != null ) {
			return parameter.getHibernateType();
		}

		Class<?> parameterJavaType = parameter.getParameterType();

		if ( parameterJavaType == null ) {
			if ( binding.isMultiValued() ) {

			}
			else {
				final Object bindValue = binding.getBindValue();
				if ( bindValue != null ) {
					parameterJavaType = bindValue.getClass();
				}
			}
		}

		if ( parameterJavaType != null ) {
			return typeConfiguration.standardExpressableTypeForJavaType( parameterJavaType );
		}

		// what else?

		throw new IllegalStateException( "Unable to determine parameter type : " + parameter );
	}
}
