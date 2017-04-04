/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.plan.exec.process.internal.AbstractRowReader;
import org.hibernate.loader.plan.exec.process.internal.CollectionReferenceInitializerImpl;
import org.hibernate.loader.plan.exec.process.internal.CollectionReturnReader;
import org.hibernate.loader.plan.exec.process.internal.EntityReferenceInitializerImpl;
import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessingContextImpl;
import org.hibernate.loader.plan.exec.process.spi.CollectionReferenceInitializer;
import org.hibernate.loader.plan.exec.process.spi.ReaderCollector;
import org.hibernate.loader.plan.exec.process.spi.RowReader;
import org.hibernate.loader.plan.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan.spi.CollectionQuerySpace;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * Handles interpreting a LoadPlan (for loading of a collection) by:<ul>
 *     <li>generating the SQL query to perform</li>
 *     <li>creating the readers needed to read the results from the SQL's ResultSet</li>
 * </ul>
 *
 * @author Gail Badner
 */
public abstract class AbstractCollectionLoadQueryDetails extends AbstractLoadQueryDetails {
	private final CollectionReferenceAliases collectionReferenceAliases;
	private final ReaderCollector readerCollector;

	protected AbstractCollectionLoadQueryDetails(
			LoadPlan loadPlan,
			AliasResolutionContextImpl aliasResolutionContext,
			CollectionReturn rootReturn,
			QueryBuildingParameters buildingParameters,
			SessionFactoryImplementor factory) {
		super(
				loadPlan,
				aliasResolutionContext,
				buildingParameters,
				( (QueryableCollection) rootReturn.getCollectionPersister() ).getKeyColumnNames(),
				rootReturn,
				factory
		);
		final String elementUid = rootReturn.getCollectionPersister().getElementType().isEntityType() ?
				rootReturn.getElementGraph().getQuerySpaceUid() :
				null;

		this.collectionReferenceAliases = aliasResolutionContext.generateCollectionReferenceAliases(
				rootReturn.getQuerySpaceUid(),
				rootReturn.getCollectionPersister(),
				elementUid
		);
		this.readerCollector = new CollectionLoaderReaderCollectorImpl(
				new CollectionReturnReader( rootReturn ),
				new CollectionReferenceInitializerImpl( rootReturn, collectionReferenceAliases )
		);
		if ( rootReturn.allowElementJoin() && rootReturn.getCollectionPersister().getElementType().isEntityType() ) {
			final EntityReference elementEntityReference = rootReturn.getElementGraph().resolveEntityReference();
			readerCollector.add(
				new EntityReferenceInitializerImpl( elementEntityReference, collectionReferenceAliases.getEntityElementAliases() )
			);
		}
		if ( rootReturn.allowIndexJoin() && rootReturn.getCollectionPersister().getIndexType().isEntityType() ) {
			final EntityReference indexEntityReference = rootReturn.getIndexGraph().resolveEntityReference();
			final EntityReferenceAliases indexEntityReferenceAliases = aliasResolutionContext.generateEntityReferenceAliases(
					indexEntityReference.getQuerySpaceUid(),
					indexEntityReference.getEntityPersister()
			);
			readerCollector.add(
					new EntityReferenceInitializerImpl( indexEntityReference, indexEntityReferenceAliases )
			);
		}
	}

	protected CollectionReturn getRootCollectionReturn() {
		return (CollectionReturn) getRootReturn();
	}

	@Override
	protected boolean isSubselectLoadingEnabled(FetchStats fetchStats) {
		return fetchStats != null && fetchStats.hasSubselectFetches();
	}

	@Override
	protected boolean shouldUseOptionalEntityInstance() {
		return false;
	}

	@Override
	protected ReaderCollector getReaderCollector() {
		return readerCollector;
	}

	@Override
	protected CollectionQuerySpace getRootQuerySpace() {
		return (CollectionQuerySpace) getQuerySpace( getRootCollectionReturn().getQuerySpaceUid() );
	}

	protected CollectionReferenceAliases getCollectionReferenceAliases() {
		return collectionReferenceAliases;
	}

	protected QueryableCollection getQueryableCollection() {
		return (QueryableCollection) getRootCollectionReturn().getCollectionPersister();
	}

	@Override
	protected boolean shouldApplyRootReturnFilterBeforeKeyRestriction() {
		return true;
	}

	@Override
	protected  void applyRootReturnSelectFragments(SelectStatementBuilder selectStatementBuilder) {
		if ( getRootCollectionReturn().allowIndexJoin() && getQueryableCollection().getIndexType().isEntityType() ) {
			final EntityReference indexEntityReference = getRootCollectionReturn().getIndexGraph().resolveEntityReference();
			final EntityReferenceAliases indexEntityReferenceAliases = getAliasResolutionContext().resolveEntityReferenceAliases(
					indexEntityReference.getQuerySpaceUid()
			);
			selectStatementBuilder.appendSelectClauseFragment(
					( (OuterJoinLoadable) indexEntityReference.getEntityPersister() ).selectFragment(
							indexEntityReferenceAliases.getTableAlias(),
							indexEntityReferenceAliases.getColumnAliases().getSuffix()
					)
			);
		}
	}

	@Override
	protected void applyRootReturnFilterRestrictions(SelectStatementBuilder selectStatementBuilder) {
		selectStatementBuilder.appendRestrictions(
				getQueryableCollection().filterFragment(
						getRootTableAlias(),
						getQueryBuildingParameters().getQueryInfluencers().getEnabledFilters()
				)
		);
	}

	@Override
	protected void applyRootReturnWhereJoinRestrictions(SelectStatementBuilder selectStatementBuilder) {
	}

	@Override
	protected void applyRootReturnOrderByFragments(SelectStatementBuilder selectStatementBuilder) {
		final String ordering = getQueryableCollection().getSQLOrderByString( getRootTableAlias() );
		if ( StringHelper.isNotEmpty( ordering ) ) {
			selectStatementBuilder.appendOrderByFragment( ordering );
		}
	}

	private static class CollectionLoaderReaderCollectorImpl extends ReaderCollectorImpl {
		private final CollectionReturnReader collectionReturnReader;

		public CollectionLoaderReaderCollectorImpl(
				CollectionReturnReader collectionReturnReader,
				CollectionReferenceInitializer collectionReferenceInitializer) {
			this.collectionReturnReader = collectionReturnReader;
			add( collectionReferenceInitializer );
		}

		@Override
		public RowReader buildRowReader() {
			return new CollectionLoaderRowReader( this );
		}

		@Override
		public CollectionReturnReader getReturnReader() {
			return collectionReturnReader;
		}
	}

	private static class CollectionLoaderRowReader extends AbstractRowReader {
		private final CollectionReturnReader rootReturnReader;

		public CollectionLoaderRowReader(CollectionLoaderReaderCollectorImpl collectionLoaderReaderCollector) {
			super( collectionLoaderReaderCollector );
			this.rootReturnReader = collectionLoaderReaderCollector.getReturnReader();
		}

		@Override
		protected Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
			return rootReturnReader.read( resultSet, context );
		}
	}
}
