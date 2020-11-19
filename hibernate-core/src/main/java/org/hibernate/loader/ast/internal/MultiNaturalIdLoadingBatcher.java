/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstSelectTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;

/**
 * Batch support for natural-id multi loading
 */
public class MultiNaturalIdLoadingBatcher {

	@FunctionalInterface
	interface KeyValueResolver {
		/**
		 * Resolve the supported forms of representing the natural-id value to
		 * the "true" form - single value for simple natural-ids and an array for
		 * compound natural-ids.
		 *
		 * Generally delegates to {@link org.hibernate.metamodel.mapping.NaturalIdMapping#normalizeValue}
		 */
		Object resolveKeyToLoad(Object incoming, SharedSessionContractImplementor session);
	}

	private final EntityMappingType entityDescriptor;

	private final SelectStatement sqlSelect;
	private final List<JdbcParameter> jdbcParameters;

	private final KeyValueResolver keyValueResolver;

	private final JdbcSelect jdbcSelect;

	public MultiNaturalIdLoadingBatcher(
			EntityMappingType entityDescriptor,
			ModelPart restrictedPart,
			int batchSize,
			KeyValueResolver keyValueResolver,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;

		jdbcParameters = new ArrayList<>( batchSize );
		sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				// return the full entity rather than parts
				null,
				restrictedPart,
				// no "cached" DomainResult
				null,
				batchSize,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameters::add,
				sessionFactory
		);

		this.keyValueResolver = keyValueResolver;

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
		final SqlAstSelectTranslator sqlAstTranslator = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory );
		this.jdbcSelect = sqlAstTranslator.translate( sqlSelect );
	}

	public <E> List<E> multiLoad(Object[] naturalIdValues, MultiNaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		final ArrayList<E> multiLoadResults = CollectionHelper.arrayList( naturalIdValues.length );

		JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		Iterator<JdbcParameter> jdbcParamItr = jdbcParameters.iterator();

		boolean needsExecution = false;

		for ( int i = 0; i < naturalIdValues.length; i++ ) {
			final JdbcParameterBindings jdbcParamBindingsRef = jdbcParamBindings;
			final Iterator<JdbcParameter> jdbcParamItrRef = jdbcParamItr;

			final Object bindValue = keyValueResolver.resolveKeyToLoad( naturalIdValues[ i ], session );
			if ( bindValue != null ) {
				entityDescriptor.getNaturalIdMapping().visitJdbcValues(
						bindValue,
						Clause.IRRELEVANT,
						(jdbcValue, jdbcMapping) -> jdbcParamBindingsRef.addBinding(
								jdbcParamItrRef.next(),
								new JdbcParameterBindingImpl( jdbcMapping, jdbcValue )
						),
						session
				);
				needsExecution = true;
			}

			if ( ! jdbcParamItr.hasNext() ) {
				// we've hit the batch mark
				final List<E> batchResults = performLoad( jdbcParamBindings, session );
				multiLoadResults.addAll( batchResults );

				jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
				jdbcParamItr = jdbcParameters.iterator();

				needsExecution = false;
			}
		}

		if ( needsExecution ) {
			while ( jdbcParamItr.hasNext() ) {
				final JdbcParameterBindings jdbcParamBindingsRef = jdbcParamBindings;
				final Iterator<JdbcParameter> jdbcParamItrRef = jdbcParamItr;
				// pad the remaining parameters with null
				entityDescriptor.getNaturalIdMapping().visitJdbcValues(
						null,
						Clause.IRRELEVANT,
						(jdbcValue, jdbcMapping) -> jdbcParamBindingsRef.addBinding(
								jdbcParamItrRef.next(),
								new JdbcParameterBindingImpl( jdbcMapping, jdbcValue )
						),
						session
				);
			}
			final List<E> batchResults = performLoad( jdbcParamBindings, session );
			multiLoadResults.addAll( batchResults );
		}

		return multiLoadResults;
	}

	private <E> List<E> performLoad(JdbcParameterBindings jdbcParamBindings, SharedSessionContractImplementor session) {
		final LoadingEntityCollector loadingEntityCollector;

		if ( entityDescriptor.getEntityPersister().hasSubselectLoadableCollections() ) {
			loadingEntityCollector = new LoadingEntityCollector(
					entityDescriptor,
					sqlSelect.getQuerySpec(),
					jdbcParameters,
					jdbcParamBindings,
					session.getPersistenceContext().getBatchFetchQueue()
			);
		}
		else {
			loadingEntityCollector = null;
		}

		return JdbcSelectExecutorStandardImpl.INSTANCE.list(
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
						return null;
					}

					@Override
					public void registerLoadingEntityEntry(EntityKey entityKey, LoadingEntityEntry entry) {
						if ( loadingEntityCollector != null ) {
							loadingEntityCollector.collectLoadingEntityKey( entityKey );
						}
					}
				},
				RowTransformerPassThruImpl.instance(),
				true
		);
	}
}
