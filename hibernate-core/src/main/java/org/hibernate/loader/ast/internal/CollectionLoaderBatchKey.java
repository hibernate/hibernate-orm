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
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;

import org.jboss.logging.Logger;

/**
 * A one-time use CollectionLoader for applying a batch fetch
 *
 * @author Steve Ebersole
 */
public class CollectionLoaderBatchKey implements CollectionLoader {
	private static final Logger log = Logger.getLogger( CollectionLoaderBatchKey.class );

	private final PluralAttributeMapping attributeMapping;
	private final int batchSize;

	private final int keyJdbcCount;

	private SelectStatement batchSizeSqlAst;
	private List<JdbcParameter> batchSizeJdbcParameters;

	public CollectionLoaderBatchKey(
			PluralAttributeMapping attributeMapping,
			int batchSize,
			LoadQueryInfluencers influencers,
			SessionFactoryImplementor sessionFactory) {
		this.attributeMapping = attributeMapping;
		this.batchSize = batchSize;

		this.keyJdbcCount = attributeMapping.getKeyDescriptor().getJdbcTypeCount( sessionFactory.getTypeConfiguration() );

		this.batchSizeJdbcParameters = new ArrayList<>();
		this.batchSizeSqlAst = LoaderSelectBuilder.createSelect(
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				null,
				batchSize,
				influencers,
				LockOptions.READ,
				batchSizeJdbcParameters::add,
				sessionFactory
		);
	}

	@Override
	public PluralAttributeMapping getLoadable() {
		return attributeMapping;
	}

	@Override
	public PersistentCollection load(
			Object key,
			SharedSessionContractImplementor session) {
		final Object[] batchIds = session.getPersistenceContextInternal()
				.getBatchFetchQueue()
				.getCollectionBatch( getLoadable().getCollectionDescriptor(), key, batchSize );

		final int numberOfIds = ArrayHelper.countNonNull( batchIds );

		if ( numberOfIds == 1 ) {
			final List<JdbcParameter> jdbcParameters = new ArrayList<>( keyJdbcCount );
			final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
					attributeMapping,
					null,
					attributeMapping.getKeyDescriptor(),
					null,
					batchSize,
					session.getLoadQueryInfluencers(),
					LockOptions.READ,
					jdbcParameters::add,
					session.getFactory()
			);

			new SingleIdLoadPlan( attributeMapping.getKeyDescriptor(), sqlAst, jdbcParameters ).load( key, LockOptions.READ, session );
		}
		else {
			batchLoad( batchIds, session );
		}

		final CollectionKey collectionKey = new CollectionKey( attributeMapping.getCollectionDescriptor(), key );
		return session.getPersistenceContext().getCollection( collectionKey );
	}

	private void batchLoad(
			Object[] batchIds,
			SharedSessionContractImplementor session) {
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Batch loading collection [%s] : %s",
					getLoadable().getCollectionDescriptor().getRole(),
					batchIds
			);
		}

		int smallBatchStart = 0;
		int smallBatchLength = Math.min( batchIds.length, batchSize );

		while ( true ) {
			final List<JdbcParameter> jdbcParameters;
			final SelectStatement sqlAst;

			if ( smallBatchLength == batchSize ) {
				jdbcParameters = this.batchSizeJdbcParameters;
				sqlAst = this.batchSizeSqlAst;
			}
			else {
				jdbcParameters = new ArrayList<>();
				sqlAst = LoaderSelectBuilder.createSelect(
						getLoadable(),
						// null here means to select everything
						null,
						getLoadable().getKeyDescriptor(),
						null,
						batchIds.length,
						session.getLoadQueryInfluencers(),
						LockOptions.READ,
						jdbcParameters::add,
						session.getFactory()
				);
			}

			final SessionFactoryImplementor sessionFactory = session.getFactory();
			final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
			final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
			final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

			final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlAst );

			final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( keyJdbcCount * smallBatchLength );
			final Iterator<JdbcParameter> paramItr = jdbcParameters.iterator();

			for ( int i = smallBatchStart; i < smallBatchStart + smallBatchLength; i++ ) {
				getLoadable().getKeyDescriptor().visitJdbcValues(
						batchIds[i],
						Clause.WHERE,
						(value, type) -> {
							assert paramItr.hasNext();
							final JdbcParameter parameter = paramItr.next();
							jdbcParameterBindings.addBinding(
									parameter,
									new JdbcParameterBinding() {
										@Override
										public JdbcMapping getBindType() {
											return type;
										}

										@Override
										public Object getBindValue() {
											return value;
										}
									}
							);
						},
						session
				);

				jdbcServices.getJdbcSelectExecutor().list(
						jdbcSelect,
						jdbcParameterBindings,
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
						},
						RowTransformerPassThruImpl.instance(),
						true
				);
			}

			assert !paramItr.hasNext();

			// prepare for the next round...
			smallBatchStart += smallBatchLength;
			if ( smallBatchStart >= batchIds.length ) {
				break;
			}

			smallBatchLength = Math.min( batchIds.length - smallBatchStart, batchSize );
		}
	}
}
