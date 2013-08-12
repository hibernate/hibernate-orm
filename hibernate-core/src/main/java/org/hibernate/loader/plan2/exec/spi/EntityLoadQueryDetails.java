/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan2.exec.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan2.build.spi.LoadPlanTreePrinter;
import org.hibernate.loader.plan2.exec.internal.AliasResolutionContextImpl;
import org.hibernate.loader.plan2.exec.internal.FetchStats;
import org.hibernate.loader.plan2.exec.internal.Helper;
import org.hibernate.loader.plan2.exec.internal.LoadQueryJoinAndFetchProcessor;
import org.hibernate.loader.plan2.exec.process.internal.EntityReferenceInitializerImpl;
import org.hibernate.loader.plan2.exec.process.internal.EntityReturnReader;
import org.hibernate.loader.plan2.exec.process.internal.ResultSetProcessingContextImpl;
import org.hibernate.loader.plan2.exec.process.internal.ResultSetProcessorHelper;
import org.hibernate.loader.plan2.exec.process.internal.ResultSetProcessorImpl;
import org.hibernate.loader.plan2.exec.process.spi.AbstractRowReader;
import org.hibernate.loader.plan2.exec.process.spi.CollectionReferenceInitializer;
import org.hibernate.loader.plan2.exec.process.spi.EntityReferenceInitializer;
import org.hibernate.loader.plan2.exec.process.spi.ReaderCollector;
import org.hibernate.loader.plan2.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.loader.plan2.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan2.exec.process.spi.RowReader;
import org.hibernate.loader.plan2.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan2.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan2.spi.EntityQuerySpace;
import org.hibernate.loader.plan2.spi.EntityReturn;
import org.hibernate.loader.plan2.spi.LoadPlan;
import org.hibernate.loader.plan2.spi.QuerySpaces;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.ConditionFragment;
import org.hibernate.sql.DisjunctionFragment;
import org.hibernate.sql.InFragment;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

/**
 * Handles interpreting a LoadPlan (for loading of an entity) by:<ul>
 *     <li>generating the SQL query to perform</li>
 *     <li>creating the readers needed to read the results from the SQL's ResultSet</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class EntityLoadQueryDetails implements LoadQueryDetails {
	private static final Logger log = CoreLogging.logger( EntityLoadQueryDetails.class );

	private final LoadPlan loadPlan;

	private final String sqlStatement;
	private final ResultSetProcessor resultSetProcessor;

	@Override
	public String getSqlStatement() {
		return sqlStatement;
	}

	@Override
	public ResultSetProcessor getResultSetProcessor() {
		return resultSetProcessor;
	}

	/**
	 * Constructs a EntityLoadQueryDetails object from the given inputs.
	 *
	 * @param loadPlan The load plan
	 * @param keyColumnNames The columns to load the entity by (the PK columns or some other unique set of columns)
	 * @param buildingParameters And influencers that would affect the generated SQL (mostly we are concerned with those
	 * that add additional joins here)
	 * @param factory The SessionFactory
	 *
	 * @return The EntityLoadQueryDetails
	 */
	public static EntityLoadQueryDetails makeForBatching(
			LoadPlan loadPlan,
			String[] keyColumnNames,
			QueryBuildingParameters buildingParameters,
			SessionFactoryImplementor factory) {
		final int batchSize = buildingParameters.getBatchSize();
		final boolean shouldUseOptionalEntityInformation = batchSize == 1;

		return new EntityLoadQueryDetails(
				loadPlan,
				keyColumnNames,
				shouldUseOptionalEntityInformation,
				buildingParameters,
				factory
		);
	}

	protected EntityLoadQueryDetails(
			LoadPlan loadPlan,
			String[] keyColumnNames,
			boolean shouldUseOptionalEntityInformation,
			QueryBuildingParameters buildingParameters,
			SessionFactoryImplementor factory) {
		this.loadPlan = loadPlan;
		final AliasResolutionContextImpl aliasResolutionContext = new AliasResolutionContextImpl( factory );

//		LoadPlanTreePrinter.INSTANCE.logTree( loadPlan, aliasResolutionContext );
//		if ( log.isDebugEnabled() ) {
//			log.debug( LoadPlanTreePrinter.INSTANCE.toString( loadPlan ) );
//		}

		// There are 2 high-level requirements to perform here:
		// 	1) Determine the SQL required to carry out the given LoadPlan (and fulfill
		// 		{@code LoadQueryDetails#getSqlStatement()}).  SelectStatementBuilder collects the ongoing efforts to
		//		build the needed SQL.
		// 	2) Determine how to read information out of the ResultSet resulting from executing the indicated SQL
		//		(the SQL aliases).  ReaderCollector and friends are where this work happens, ultimately
		//		producing a ResultSetProcessor

		final SelectStatementBuilder select = new SelectStatementBuilder( factory.getDialect() );
		final EntityReturn rootReturn = Helper.INSTANCE.extractRootReturn( loadPlan, EntityReturn.class );
		final ReaderCollectorImpl readerCollector = new ReaderCollectorImpl();

		final LoadQueryJoinAndFetchProcessor helper = new LoadQueryJoinAndFetchProcessor( aliasResolutionContext , buildingParameters, factory );

		final String[] keyColumnNamesToUse = keyColumnNames != null
				? keyColumnNames
				: ( (Queryable) rootReturn.getEntityPersister() ).getIdentifierColumnNames();

		// LoadPlan is broken down into 2 high-level pieces that we need to process here.
		//
		// First is the QuerySpaces, which roughly equates to the SQL FROM-clause.  We'll cycle through
		// those first, generating aliases into the AliasContext in addition to writing SQL FROM-clause information
		// into SelectStatementBuilder.  The AliasContext is populated here and the reused while process the SQL
		// SELECT-clause into the SelectStatementBuilder and then again also to build the ResultSetProcessor

		processQuerySpaces(
				loadPlan.getQuerySpaces(),
				select,
				keyColumnNamesToUse,
				helper,
				aliasResolutionContext,
				buildingParameters,
				factory
		);

		// Next, we process the Returns and Fetches building the SELECT clause and at the same time building
		// Readers for reading the described results out of a SQL ResultSet

		final FetchStats fetchStats = processReturnAndFetches(
				rootReturn,
				select,
				helper,
				readerCollector,
				aliasResolutionContext
		);

		LoadPlanTreePrinter.INSTANCE.logTree( loadPlan, aliasResolutionContext );

		this.sqlStatement = select.toStatementString();
		this.resultSetProcessor = new ResultSetProcessorImpl(
				loadPlan,
				readerCollector.buildRowReader(),
				fetchStats.hasSubselectFetches()
		);
	}

	/**
	 * Main entry point for handling building the SQL SELECT clause and the corresponding Readers,
	 *
	 * @param rootReturn The root return reference we are processing
	 * @param select The SelectStatementBuilder
	 * @param helper The Join/Fetch helper
	 * @param readerCollector Collector for EntityReferenceInitializer and CollectionReferenceInitializer references
	 * @param aliasResolutionContext The alias resolution context
	 *
	 * @return Stats about the processed fetches
	 */
	private FetchStats processReturnAndFetches(
			EntityReturn rootReturn,
			SelectStatementBuilder select,
			LoadQueryJoinAndFetchProcessor helper,
			ReaderCollectorImpl readerCollector,
			AliasResolutionContextImpl aliasResolutionContext) {
		final EntityReferenceAliases entityReferenceAliases = aliasResolutionContext.resolveEntityReferenceAliases(
				rootReturn.getQuerySpaceUid()
		);

		final OuterJoinLoadable rootLoadable = (OuterJoinLoadable) rootReturn.getEntityPersister();

		// add the root persister SELECT fragments...
		select.appendSelectClauseFragment(
				rootLoadable.selectFragment(
						entityReferenceAliases.getTableAlias(),
						entityReferenceAliases.getColumnAliases().getSuffix()
				)
		);

		final FetchStats fetchStats = helper.processFetches(
				rootReturn,
				select,
				readerCollector
		);

		readerCollector.setRootReturnReader( new EntityReturnReader( rootReturn, entityReferenceAliases ) );
		readerCollector.add( new EntityReferenceInitializerImpl( rootReturn, entityReferenceAliases, true ) );

		return fetchStats;
	}


	/**
	 * Main entry point for properly handling the FROM clause and and joins and restrictions
	 *
	 * @param querySpaces The QuerySpaces
	 * @param select The SelectStatementBuilder
	 * @param keyColumnNamesToUse The column names to use from the entity table (space) in crafting the entity restriction
	 * (which entity/entities are we interested in?)
	 * @param helper The Join/Fetch helper
	 * @param aliasResolutionContext yadda
	 * @param buildingParameters yadda
	 * @param factory yadda
	 */
	private void processQuerySpaces(
			QuerySpaces querySpaces,
			SelectStatementBuilder select,
			String[] keyColumnNamesToUse,
			LoadQueryJoinAndFetchProcessor helper,
			AliasResolutionContextImpl aliasResolutionContext,
			QueryBuildingParameters buildingParameters,
			SessionFactoryImplementor factory) {
		// Should be just one querySpace (of type EntityQuerySpace) in querySpaces.  Should we validate that?
		// Should we make it a util method on Helper like we do for extractRootReturn ?
		final EntityQuerySpace rootQuerySpace = Helper.INSTANCE.extractRootQuerySpace(
				querySpaces,
				EntityQuerySpace.class
		);

		final EntityReferenceAliases entityReferenceAliases = aliasResolutionContext.generateEntityReferenceAliases(
				rootQuerySpace.getUid(),
				rootQuerySpace.getEntityPersister()
		);

		final String rootTableAlias = entityReferenceAliases.getTableAlias();
		applyTableFragments(
				select,
				factory,
				buildingParameters,
				rootTableAlias,
				(OuterJoinLoadable) rootQuerySpace.getEntityPersister()
		);

		// add restrictions...
		// first, the load key restrictions (which entity(s) do we want to load?)
		applyKeyRestriction(
				select,
				entityReferenceAliases.getTableAlias(),
				keyColumnNamesToUse,
				buildingParameters.getBatchSize()
		);

		// don't quite remember why these 2 anymore, todo : research that and document this code or remove it etc..
		final OuterJoinLoadable rootLoadable = (OuterJoinLoadable) rootQuerySpace.getEntityPersister();
		final Queryable rootQueryable = (Queryable) rootQuerySpace.getEntityPersister();
		select.appendRestrictions(
				rootQueryable.filterFragment(
						entityReferenceAliases.getTableAlias(),
						Collections.emptyMap()
				)
		);
		select.appendRestrictions(
				rootLoadable.whereJoinFragment(
						entityReferenceAliases.getTableAlias(),
						true,
						true
				)
		);

		// then move on to joins...
		helper.processQuerySpaceJoins( rootQuerySpace, select );
	}


	/**
	 * Applies "table fragments" to the FROM-CLAUSE of the given SelectStatementBuilder for the given Loadable
	 *
	 * @param select The SELECT statement builder
	 * @param factory The SessionFactory
	 * @param buildingParameters The query building context
	 * @param rootAlias The table alias to use
	 * @param rootLoadable The persister
	 *
	 * @see org.hibernate.persister.entity.OuterJoinLoadable#fromTableFragment(java.lang.String)
	 * @see org.hibernate.persister.entity.Joinable#fromJoinFragment(java.lang.String, boolean, boolean)
	 */
	private void applyTableFragments(
			SelectStatementBuilder select,
			SessionFactoryImplementor factory,
			QueryBuildingParameters buildingParameters,
			String rootAlias,
			OuterJoinLoadable rootLoadable) {
		final String fromTableFragment;
		if ( buildingParameters.getLockOptions() != null ) {
			fromTableFragment = factory.getDialect().appendLockHint(
					buildingParameters.getLockOptions(),
					rootLoadable.fromTableFragment( rootAlias )
			);
			select.setLockOptions( buildingParameters.getLockOptions() );
		}
		else if ( buildingParameters.getLockMode() != null ) {
			fromTableFragment = factory.getDialect().appendLockHint(
					buildingParameters.getLockMode(),
					rootLoadable.fromTableFragment( rootAlias )
			);
			select.setLockMode( buildingParameters.getLockMode() );
		}
		else {
			fromTableFragment = rootLoadable.fromTableFragment( rootAlias );
		}
		select.appendFromClauseFragment( fromTableFragment + rootLoadable.fromJoinFragment( rootAlias, true, true ) );
	}

	private static class ReaderCollectorImpl implements ReaderCollector {
		private EntityReturnReader rootReturnReader;
		private final List<EntityReferenceInitializer> entityReferenceInitializers = new ArrayList<EntityReferenceInitializer>();
		private List<CollectionReferenceInitializer> arrayReferenceInitializers;
		private List<CollectionReferenceInitializer> collectionReferenceInitializers;

		@Override
		public void add(CollectionReferenceInitializer collectionReferenceInitializer) {
			if ( collectionReferenceInitializer.getCollectionReference().getCollectionPersister().isArray() ) {
				if ( arrayReferenceInitializers == null ) {
					arrayReferenceInitializers = new ArrayList<CollectionReferenceInitializer>();
				}
				arrayReferenceInitializers.add( collectionReferenceInitializer );
			}
			else {
				if ( collectionReferenceInitializers == null ) {
					collectionReferenceInitializers = new ArrayList<CollectionReferenceInitializer>();
				}
				collectionReferenceInitializers.add( collectionReferenceInitializer );
			}
		}

		@Override
		public void add(EntityReferenceInitializer entityReferenceInitializer) {
			if ( EntityReturnReader.class.isInstance( entityReferenceInitializer ) ) {
				setRootReturnReader( (EntityReturnReader) entityReferenceInitializer );
			}
			entityReferenceInitializers.add( entityReferenceInitializer );
		}

		public RowReader buildRowReader() {
			return new EntityLoaderRowReader(
					rootReturnReader,
					entityReferenceInitializers,
					arrayReferenceInitializers,
					collectionReferenceInitializers
			);
		}

		public void setRootReturnReader(EntityReturnReader entityReturnReader) {
			if ( rootReturnReader != null ) {
				throw new IllegalStateException( "Root return reader already set" );
			}
			rootReturnReader = entityReturnReader;

		}
	}

	public static class EntityLoaderRowReader extends AbstractRowReader {
		private final EntityReturnReader rootReturnReader;
		private final List<EntityReferenceInitializer> entityReferenceInitializers;
		private final List<CollectionReferenceInitializer> arrayReferenceInitializers;
		private final List<CollectionReferenceInitializer> collectionReferenceInitializers;

		public EntityLoaderRowReader(
				EntityReturnReader rootReturnReader,
				List<EntityReferenceInitializer> entityReferenceInitializers,
				List<CollectionReferenceInitializer> arrayReferenceInitializers,
				List<CollectionReferenceInitializer> collectionReferenceInitializers) {
			this.rootReturnReader = rootReturnReader;
			this.entityReferenceInitializers = entityReferenceInitializers != null
					? entityReferenceInitializers
					: Collections.<EntityReferenceInitializer>emptyList();
			this.arrayReferenceInitializers = arrayReferenceInitializers != null
					? arrayReferenceInitializers
					: Collections.<CollectionReferenceInitializer>emptyList();
			this.collectionReferenceInitializers = collectionReferenceInitializers != null
					? collectionReferenceInitializers
					: Collections.<CollectionReferenceInitializer>emptyList();
		}

		@Override
		public Object readRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
			final ResultSetProcessingContext.EntityReferenceProcessingState processingState =
					rootReturnReader.getIdentifierResolutionContext( context );
			// if the entity reference we are hydrating is a Return, it is possible that its EntityKey is
			// supplied by the QueryParameter optional entity information
			if ( context.shouldUseOptionalEntityInformation() && context.getQueryParameters().getOptionalId() != null ) {
				EntityKey entityKey = ResultSetProcessorHelper.getOptionalObjectKey(
						context.getQueryParameters(),
						context.getSession()
				);
				processingState.registerIdentifierHydratedForm( entityKey.getIdentifier() );
				processingState.registerEntityKey( entityKey );
				final EntityPersister entityPersister = processingState.getEntityReference().getEntityPersister();
				if ( entityPersister.getIdentifierType().isComponentType()  ) {
					final ComponentType identifierType = (ComponentType) entityPersister.getIdentifierType();
					if ( !identifierType.isEmbedded() ) {
						addKeyManyToOnesToSession(
								context,
								identifierType,
								entityKey.getIdentifier()
						);
					}
				}
			}
			return super.readRow( resultSet, context );
		}

		private void addKeyManyToOnesToSession(ResultSetProcessingContextImpl context, ComponentType componentType, Object component ) {
			for ( int i = 0 ; i < componentType.getSubtypes().length ; i++ ) {
				final Type subType = componentType.getSubtypes()[ i ];
				final Object subValue = componentType.getPropertyValue( component, i, context.getSession() );
				if ( subType.isEntityType() ) {
					( (Session) context.getSession() ).buildLockRequest( LockOptions.NONE ).lock( subValue );
				}
				else if ( subType.isComponentType() ) {
					addKeyManyToOnesToSession( context, (ComponentType) subType, subValue  );
				}
			}
		}

		@Override
		protected List<EntityReferenceInitializer> getEntityReferenceInitializers() {
			return entityReferenceInitializers;
		}

		@Override
		protected List<CollectionReferenceInitializer> getCollectionReferenceInitializers() {
			return collectionReferenceInitializers;
		}

		@Override
		protected List<CollectionReferenceInitializer> getArrayReferenceInitializers() {
			return arrayReferenceInitializers;
		}

		@Override
		protected Object readLogicalRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {
			return rootReturnReader.read( resultSet, context );
		}
	}

	private static void applyKeyRestriction(SelectStatementBuilder select, String alias, String[] keyColumnNames, int batchSize) {
		if ( keyColumnNames.length==1 ) {
			// NOT A COMPOSITE KEY
			// 		for batching, use "foo in (?, ?, ?)" for batching
			//		for no batching, use "foo = ?"
			// (that distinction is handled inside InFragment)
			final InFragment in = new InFragment().setColumn( alias, keyColumnNames[0] );
			for ( int i = 0; i < batchSize; i++ ) {
				in.addValue( "?" );
			}
			select.appendRestrictions( in.toFragmentString() );
		}
		else {
			// A COMPOSITE KEY...
			final ConditionFragment keyRestrictionBuilder = new ConditionFragment()
					.setTableAlias( alias )
					.setCondition( keyColumnNames, "?" );
			final String keyRestrictionFragment = keyRestrictionBuilder.toFragmentString();

			StringBuilder restrictions = new StringBuilder();
			if ( batchSize==1 ) {
				// for no batching, use "foo = ? and bar = ?"
				restrictions.append( keyRestrictionFragment );
			}
			else {
				// for batching, use "( (foo = ? and bar = ?) or (foo = ? and bar = ?) )"
				restrictions.append( '(' );
				DisjunctionFragment df = new DisjunctionFragment();
				for ( int i=0; i<batchSize; i++ ) {
					df.addCondition( keyRestrictionFragment );
				}
				restrictions.append( df.toFragmentString() );
				restrictions.append( ')' );
			}
			select.appendRestrictions( restrictions.toString() );
		}
	}
}
