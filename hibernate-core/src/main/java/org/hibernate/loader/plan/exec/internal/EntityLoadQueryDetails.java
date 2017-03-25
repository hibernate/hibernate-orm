/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.exec.process.internal.AbstractRowReader;
import org.hibernate.loader.plan.exec.process.internal.EntityReferenceInitializerImpl;
import org.hibernate.loader.plan.exec.process.internal.EntityReturnReader;
import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessingContextImpl;
import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessorHelper;
import org.hibernate.loader.plan.exec.process.spi.EntityReferenceInitializer;
import org.hibernate.loader.plan.exec.process.spi.ReaderCollector;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.loader.plan.exec.process.spi.RowReader;
import org.hibernate.loader.plan.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Handles interpreting a LoadPlan (for loading of an entity) by:<ul>
 *     <li>generating the SQL query to perform</li>
 *     <li>creating the readers needed to read the results from the SQL's ResultSet</li>
 * </ul>
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class EntityLoadQueryDetails extends AbstractLoadQueryDetails {
	private static final Logger log = CoreLogging.logger( EntityLoadQueryDetails.class );

	private final EntityReferenceAliases entityReferenceAliases;
	private final ReaderCollector readerCollector;

	/**
	 * Constructs a EntityLoadQueryDetails object from the given inputs.
	 *
	 * @param loadPlan The load plan
	 * @param keyColumnNames The columns to load the entity by (the PK columns or some other unique set of columns)
	 * @param buildingParameters And influencers that would affect the generated SQL (mostly we are concerned with those
	 * that add additional joins here)
	 * @param factory The SessionFactory
	 */
	protected EntityLoadQueryDetails(
			LoadPlan loadPlan,
			String[] keyColumnNames,
			AliasResolutionContextImpl aliasResolutionContext,
			EntityReturn rootReturn,
			QueryBuildingParameters buildingParameters,
			SessionFactoryImplementor factory) {
		super(
				loadPlan,
				aliasResolutionContext,
				buildingParameters,
				keyColumnNames,
				rootReturn,
				factory
		);
		this.entityReferenceAliases = aliasResolutionContext.generateEntityReferenceAliases(
				rootReturn.getQuerySpaceUid(),
				rootReturn.getEntityPersister()
		);
		this.readerCollector = new EntityLoaderReaderCollectorImpl(
				new EntityReturnReader( rootReturn ),
				new EntityReferenceInitializerImpl( rootReturn, entityReferenceAliases, true )
		);
		generate();
	}

	private EntityReturn getRootEntityReturn() {
		return (EntityReturn) getRootReturn();
	}

	/**
	 * Applies "table fragments" to the FROM-CLAUSE of the given SelectStatementBuilder for the given Loadable
	 *
	 * @param select The SELECT statement builder
	 *
	 * @see org.hibernate.persister.entity.OuterJoinLoadable#fromTableFragment(java.lang.String)
	 * @see org.hibernate.persister.entity.Joinable#fromJoinFragment(java.lang.String, boolean, boolean)
	 */
	protected void applyRootReturnTableFragments(SelectStatementBuilder select) {
		final String fromTableFragment;
		final String rootAlias = entityReferenceAliases.getTableAlias();
		final OuterJoinLoadable outerJoinLoadable = (OuterJoinLoadable) getRootEntityReturn().getEntityPersister();
		final Dialect dialect = getSessionFactory().getJdbcServices().getJdbcEnvironment().getDialect();
		if ( getQueryBuildingParameters().getLockOptions() != null ) {
			fromTableFragment = dialect.appendLockHint(
					getQueryBuildingParameters().getLockOptions(),
					outerJoinLoadable.fromTableFragment( rootAlias )
			);
			select.setLockOptions( getQueryBuildingParameters().getLockOptions() );
		}
		else if ( getQueryBuildingParameters().getLockMode() != null ) {
			fromTableFragment = dialect.appendLockHint(
					getQueryBuildingParameters().getLockMode(),
					outerJoinLoadable.fromTableFragment( rootAlias )
			);
			select.setLockMode( getQueryBuildingParameters().getLockMode() );
		}
		else {
			fromTableFragment = outerJoinLoadable.fromTableFragment( rootAlias );
		}
		select.appendFromClauseFragment( fromTableFragment + outerJoinLoadable.fromJoinFragment( rootAlias, true, true ) );
	}

	protected void applyRootReturnFilterRestrictions(SelectStatementBuilder selectStatementBuilder) {
		final Queryable rootQueryable = (Queryable) getRootEntityReturn().getEntityPersister();
		selectStatementBuilder.appendRestrictions(
				rootQueryable.filterFragment(
						entityReferenceAliases.getTableAlias(),
						Collections.emptyMap()
				)
		);
	}

	protected void applyRootReturnWhereJoinRestrictions(SelectStatementBuilder selectStatementBuilder) {
		final Joinable joinable = (OuterJoinLoadable) getRootEntityReturn().getEntityPersister();
		selectStatementBuilder.appendRestrictions(
				joinable.whereJoinFragment(
						entityReferenceAliases.getTableAlias(),
						true,
						true
				)
		);
	}

	@Override
	protected void applyRootReturnOrderByFragments(SelectStatementBuilder selectStatementBuilder) {
	}

	@Override
	protected boolean isSubselectLoadingEnabled(FetchStats fetchStats) {
		return getQueryBuildingParameters().getBatchSize() > 1 &&
				fetchStats != null &&
				fetchStats.hasSubselectFetches();
	}

	@Override
	protected boolean shouldUseOptionalEntityInstance() {
		return getQueryBuildingParameters().getBatchSize() < 2;
	}

	@Override
	protected ReaderCollector getReaderCollector() {
		return readerCollector;
	}

	@Override
	protected QuerySpace getRootQuerySpace() {
		return getQuerySpace( getRootEntityReturn().getQuerySpaceUid() );
	}

	@Override
	protected String getRootTableAlias() {
		return entityReferenceAliases.getTableAlias();
	}

	@Override
	protected boolean shouldApplyRootReturnFilterBeforeKeyRestriction() {
		return false;
	}

	protected void applyRootReturnSelectFragments(SelectStatementBuilder selectStatementBuilder) {
		final OuterJoinLoadable outerJoinLoadable = (OuterJoinLoadable) getRootEntityReturn().getEntityPersister();
		selectStatementBuilder.appendSelectClauseFragment(
				outerJoinLoadable.selectFragment(
						entityReferenceAliases.getTableAlias(),
						entityReferenceAliases.getColumnAliases().getSuffix()

				)
		);
	}

	private static class EntityLoaderReaderCollectorImpl extends ReaderCollectorImpl {
		private final EntityReturnReader entityReturnReader;

		public EntityLoaderReaderCollectorImpl(
				EntityReturnReader entityReturnReader,
				EntityReferenceInitializer entityReferenceInitializer) {
			this.entityReturnReader = entityReturnReader;
			add( entityReferenceInitializer );
		}

		@Override
		public RowReader buildRowReader() {
			return new EntityLoaderRowReader( this );
		}

		@Override
		public EntityReturnReader getReturnReader() {
			return entityReturnReader;
		}
	}

	private static class EntityLoaderRowReader extends AbstractRowReader {
		private final EntityReturnReader rootReturnReader;

		public EntityLoaderRowReader(EntityLoaderReaderCollectorImpl entityLoaderReaderCollector) {
			super( entityLoaderReaderCollector );
			this.rootReturnReader = entityLoaderReaderCollector.getReturnReader();
		}

		@Override
		public Object readRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
			final ResultSetProcessingContext.EntityReferenceProcessingState processingState =
					rootReturnReader.getIdentifierResolutionContext( context );
			// if the entity reference we are hydrating is a Return, it is possible that its EntityKey is
			// supplied by the QueryParameter optional entity information
			if ( context.shouldUseOptionalEntityInformation() && context.getQueryParameters().getOptionalId() != null ) {
				final EntityKey entityKey = context.getSession().generateEntityKey(
						context.getQueryParameters().getOptionalId(),
						processingState.getEntityReference().getEntityPersister()
				);
				processingState.registerIdentifierHydratedForm( entityKey.getIdentifier() );
				processingState.registerEntityKey( entityKey );
			}
			return super.readRow( resultSet, context );
		}

		@Override
		protected Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
			return rootReturnReader.read( resultSet, context );
		}
	}
}
