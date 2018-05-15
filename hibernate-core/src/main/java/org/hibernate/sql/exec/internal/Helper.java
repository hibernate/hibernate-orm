/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.internal.StandardSqlExpressionResolver;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.RowTransformer;
import org.hibernate.sql.results.SqlResultsLogger;
import org.hibernate.sql.results.internal.RowReaderStandardImpl;
import org.hibernate.sql.results.internal.values.JdbcValues;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.RowReader;

/**
 * @author Steve Ebersole
 */
public class Helper {

	public static final SqlExpressionResolver SQL_EXPRESSION_RESOLVER = new StandardSqlExpressionResolver(
			() -> null,
			expression -> expression,
			(expression, sqlSelection) -> {
			}
	);

	public static <R> RowReader<R> createRowReader(
			SessionFactoryImplementor sessionFactory,
			Callback callback,
			RowTransformer<R> rowTransformer,
			JdbcValues jdbcValues) {
		final List<Initializer> initializers = new ArrayList<>();

		final List<DomainResultAssembler> assemblers = jdbcValues.getResultSetMapping().resolveAssemblers(
				getInitializerConsumer( initializers ),
				new AssemblerCreationState() {
					@Override
					public LoadQueryInfluencers getLoadQueryInfluencers() {
						return LoadQueryInfluencers.NONE;
					}

					@Override
					public SqlExpressionResolver getSqlExpressionResolver() {
						return SQL_EXPRESSION_RESOLVER;
					}

					@Override
					public ColumnReferenceQualifier getCurrentColumnReferenceQualifier() {
						return null;
					}

					@Override
					public LockOptions getLockOptions() {
						return null;
					}

					@Override
					public boolean shouldCreateShallowEntityResult() {
						return false;
					}
				},
				() -> sessionFactory
		);

		return new RowReaderStandardImpl<>(
				assemblers,
				initializers,
				rowTransformer,
				callback
		);
	}

	private static Consumer<Initializer> getInitializerConsumer(List<Initializer> initializers) {
		if ( SqlResultsLogger.INSTANCE.isDebugEnabled() ) {
			return initializer -> {
				// noinspection Convert2MethodRef
				SqlResultsLogger.INSTANCE.debug( "Adding initializer : " + initializer );
				initializers.add( initializer );
			};
		}
		else {
			return initializer -> {
				// noinspection Convert2MethodRef
				initializers.add( initializer );
			};
		}
	}


	public static JdbcParameterBindings createJdbcParameterBindings(
			QueryParameterBindings<QueryParameterBinding<?>> domainParamBindings,
			Map<QueryParameterImplementor,List<JdbcParameter>> jdbcParamsByDomainParams,
			SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();

		domainParamBindings.visitBindings(
				(queryParameterImplementor, queryParameterBinding) -> {
					final List<JdbcParameter> jdbcParameters = jdbcParamsByDomainParams.get( queryParameterImplementor );
					final Object bindValue = domainParamBindings.getBinding( queryParameterImplementor ).getBindValue();
					queryParameterBinding.getBindType().dehydrate(
							queryParameterBinding.getBindType().unresolve( bindValue, session ),
							new ExpressableType.JdbcValueCollector() {
								private int position = 0;

								@Override
								public void collect(Object jdbcValue, SqlExpressableType type, Column boundColumn) {
									final JdbcParameter jdbcParameter = jdbcParameters.get( position );
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
		);
//		for ( Map.Entry<QueryParameterImplementor, List<JdbcParameter>> entry : jdbcParamsByDomainParams.entrySet() ) {
//			final QueryParameterBinding<?> binding = domainParamBindings.getBinding( entry.getKey() );
//			binding.getBindType().dehydrate(
//					binding.getBindType().unresolve( binding.getBindValue(), session ),
//					new Writeable.JdbcValueCollector() {
//						private int position = 0;
//
//						@Override
//						public void collect(Object jdbcValue, SqlExpressableType type, Column boundColumn) {
//							jdbcParameterBindings.addBinding(
//									entry.getValue().get( position ),
//									new JdbcParameterBinding() {
//										@Override
//										public SqlExpressableType getBindType() {
//											return type;
//										}
//
//										@Override
//										public Object getBindValue() {
//											return jdbcValue;
//										}
//									}
//							);
//						}
//					},
//					clause,
//					session
//			);
//		}

		return jdbcParameterBindings;
	}
}
