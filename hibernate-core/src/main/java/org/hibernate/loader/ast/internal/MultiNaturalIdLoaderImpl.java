/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
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
import org.hibernate.sql.results.LoadingLogger;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;

/**
 * Standard MultiNaturalIdLoader implementation
 */
public class MultiNaturalIdLoaderImpl<E> implements MultiNaturalIdLoader<E> {

	// todo (6.0) : much of the execution logic here is borrowed from `org.hibernate.loader.ast.internal.MultiIdEntityLoaderStandardImpl`
	// 	- consider ways to consolidate/share logic

	private final EntityMappingType entityDescriptor;

	public MultiNaturalIdLoaderImpl(EntityMappingType entityDescriptor, MappingModelCreationProcess creationProcess) {
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	public <K> List<E> multiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		if ( naturalIds == null ) {
			throw new IllegalArgumentException( "Natural-ids passed to load is null" );
		}

		if ( options.isOrderReturnEnabled() ) {
			return performOrderedMultiLoad( naturalIds, options, session );
		}
		else {
			return performUnorderedMultiLoad( naturalIds, options, session );
		}
	}

	private <K> List<E> performOrderedMultiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		assert options.isOrderReturnEnabled();

		throw new NotYetImplementedFor6Exception( "`#performOrderedMultiLoad` not yet implemented" );
	}

	private <K> List<E> performUnorderedMultiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		if ( naturalIds.length == 0 ) {
			return Collections.emptyList();
		}

		if ( LoadingLogger.LOGGER.isTraceEnabled() ) {
			LoadingLogger.LOGGER.tracef( "Starting unordered multi natural-id loading for `%s`", entityDescriptor.getEntityName() );
		}

		final SessionFactoryImplementor sessionFactory = session.getFactory();

		final List<E> result = CollectionHelper.arrayList( naturalIds.length );

		final LockOptions lockOptions = (options.getLockOptions() == null)
				? new LockOptions( LockMode.NONE )
				: options.getLockOptions();

		int numberOfIdsLeft = naturalIds.length;

		final int maxBatchSize;
		if ( options.getBatchSize() != null && options.getBatchSize() > 0 ) {
			maxBatchSize = options.getBatchSize();
		}
		else {
			maxBatchSize = session.getJdbcServices().getJdbcEnvironment().getDialect().getDefaultBatchLoadSizingStrategy().determineOptimalBatchLoadSize(
					entityDescriptor.getNaturalIdMapping().getJdbcTypeCount( sessionFactory.getTypeConfiguration() ),
					numberOfIdsLeft
			);
		}

		int idPosition = 0;
		while ( numberOfIdsLeft > 0 ) {
			final int batchSize = Math.min( numberOfIdsLeft, maxBatchSize );

			final Object[] naturalIdsInBatch = new Object[ batchSize ];
			System.arraycopy( naturalIds, idPosition, naturalIdsInBatch, 0, batchSize );

			final List<E> loaded = loadEntitiesByNaturalId( naturalIdsInBatch, lockOptions, session );
			result.addAll( loaded );

			numberOfIdsLeft = numberOfIdsLeft - batchSize;
			idPosition += batchSize;
		}

		return result;
	}

	private <K> List<E> loadEntitiesByNaturalId(K[] naturalIdsInBatch, LockOptions lockOptions, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final List<JdbcParameter> jdbcParameters = new ArrayList<>();
		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				null,
				entityDescriptor.getNaturalIdMapping(),
				null,
				naturalIdsInBatch.length,
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameters::add,
				sessionFactory
		);

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlSelect );

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final Iterator<JdbcParameter> jdbcParamItr = jdbcParameters.iterator();

		for ( int i = 0; i < naturalIdsInBatch.length; i++ ) {
			final K id = naturalIdsInBatch[ i ];

			entityDescriptor.getNaturalIdMapping().visitJdbcValues(
					id,
					Clause.WHERE,
					(value, type) -> {
						assert jdbcParamItr.hasNext();
						final JdbcParameter parameter = jdbcParamItr.next();
						jdbcParamBindings.addBinding(
								parameter,
								new JdbcParameterBindingImpl( type, value )
						);
					},
					session
			);
		}

		// we should have used all of the JdbcParameter references (created bindings for all)
		assert !jdbcParamItr.hasNext();

		final SubselectFetch subselectFetch;

		if ( entityDescriptor.getEntityPersister().hasSubselectLoadableCollections() ) {
			subselectFetch = new SubselectFetch(
					entityDescriptor,
					sqlSelect.getQuerySpec(),
					sqlSelect.getQuerySpec().getFromClause().getRoots().get( 0 ),
					jdbcParameters,
					jdbcParamBindings,
					new HashSet<>()
			);
		}
		else {
			subselectFetch = null;
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
						if ( subselectFetch != null ) {
							subselectFetch.getResultingEntityKeys().add( entityKey );
						}
					}
				},
				RowTransformerPassThruImpl.instance(),
				true
		);
	}
}
