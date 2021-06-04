/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/**
 * NaturalIdLoader for simple natural-ids
 */
public class SimpleNaturalIdLoader<T> extends AbstractNaturalIdLoader<T> {

	public SimpleNaturalIdLoader(
			SimpleNaturalIdMapping naturalIdMapping,
			EntityMappingType entityDescriptor) {
		super( naturalIdMapping, entityDescriptor );
	}

	@Override
	protected SimpleNaturalIdMapping naturalIdMapping() {
		return (SimpleNaturalIdMapping) super.naturalIdMapping();
	}

	@Override
	protected void applyNaturalIdRestriction(
			Object bindValue,
			TableGroup rootTableGroup,
			Consumer<Predicate> predicateConsumer,
			BiConsumer<JdbcParameter, JdbcParameterBinding> jdbcParameterConsumer,
			LoaderSqlAstCreationState sqlAstCreationState,
			SharedSessionContractImplementor session) {
		if ( bindValue == null ) {
			naturalIdMapping().getAttribute().forEachSelectable(
					(index, selectable) -> {
						final Expression columnReference = resolveColumnReference(
								rootTableGroup,
								selectable,
								sqlAstCreationState.getSqlExpressionResolver(),
								session.getFactory()
						);
						predicateConsumer.accept( new NullnessPredicate( columnReference ) );
					}
			);
		}
		else {
			naturalIdMapping().getAttribute().breakDownJdbcValues(
					bindValue,
					(jdbcValue, jdbcValueMapping) -> {
						final Expression columnReference = resolveColumnReference(
								rootTableGroup,
								jdbcValueMapping,
								sqlAstCreationState.getSqlExpressionResolver(),
								session.getFactory()
						);
						if ( jdbcValue == null ) {
							predicateConsumer.accept( new NullnessPredicate( columnReference ) );
						}
						else {
							final JdbcParameter jdbcParameter = new JdbcParameterImpl( jdbcValueMapping.getJdbcMapping() );
							final ComparisonPredicate predicate = new ComparisonPredicate(
									columnReference,
									ComparisonOperator.EQUAL,
									jdbcParameter
							);
							predicateConsumer.accept( predicate );
							jdbcParameterConsumer.accept(
									jdbcParameter,
									new JdbcParameterBindingImpl( jdbcValueMapping.getJdbcMapping(), jdbcValue )
							);
						}
					},
					session
			);
		}
	}

	@Override
	public Object resolveIdToNaturalId(Object id, SharedSessionContractImplementor session) {
		final Object rawValue = super.resolveIdToNaturalId( id, session );
		assert rawValue instanceof Object[];

		return ( (Object[]) rawValue )[0];
	}

	@Override
	public Object resolveNaturalIdToId(
			Object naturalIdValue,
			SharedSessionContractImplementor session) {
		final Object bindValue = naturalIdMapping().normalizeInput( naturalIdValue, session );

		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final List<JdbcParameter> jdbcParameters = new ArrayList<>();
		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor(),
				Collections.singletonList( entityDescriptor().getIdentifierMapping() ),
				naturalIdMapping(),
				null,
				1,
				session.getLoadQueryInfluencers(),
				LockOptions.READ,
				jdbcParameters::add,
				sessionFactory
		);

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );

		final SingularAttributeMapping attributeMapping = naturalIdMapping().getAttribute();
		jdbcParamBindings.registerParametersForEachJdbcValue(
				bindValue,
				Clause.WHERE,
				attributeMapping,
				jdbcParameters,
				session
		);
		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, sqlSelect )
				.translate( jdbcParamBindings, QueryOptions.NONE );

		final List<?> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParamBindings,
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return QueryOptions.NONE;
					}

					@Override
					public QueryParameterBindings getQueryParameterBindings() {
						return QueryParameterBindings.NO_PARAM_BINDINGS;
					}

					@Override
					public Callback getCallback() {
						throw new UnsupportedOperationException( "Follow-on locking not supported yet" );
					}
				},
				row -> row[0],
				ListResultsConsumer.UniqueSemantic.FILTER
		);

		if ( results.size() > 1 ) {
			throw new HibernateException(
					String.format(
							"Resolving natural-id to id returned more that one row : %s [%s]",
							entityDescriptor().getEntityName(),
							bindValue
					)
			);
		}

		return results.get( 0 );
	}
}
