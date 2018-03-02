/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity.plan;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeDescriptor;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.build.internal.FetchGroupLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.exec.internal.AliasResolutionContextImpl;
import org.hibernate.loader.plan.exec.internal.EntityLoadQueryDetails;
import org.hibernate.loader.plan.exec.internal.RootHelper;
import org.hibernate.loader.plan.exec.process.internal.EntityReturnReader;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.loader.plan.exec.query.internal.QueryBuildingParametersImpl;
import org.hibernate.loader.plan.exec.query.internal.SelectStatementBuilder;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.Queryable;

import org.jboss.logging.Logger;

/**
 * @author Gail Badner
 */
public class LazyGroupLoader extends AbstractLoadPlanBasedEntityLoader {
	private static final Logger log = CoreLogging.logger( LazyGroupLoader.class );

	public static Builder forFetchGroup(OuterJoinLoadable persister, String fetchGroupName) {
		return new Builder( persister, fetchGroupName );
	}

	public static class Builder {
		private final OuterJoinLoadable persister;
		private final String fetchGroupName;
		private boolean isCacheReadEnabled;

		public Builder(OuterJoinLoadable persister, String fetchGroupName) {
			this.persister = persister;
			this.fetchGroupName = fetchGroupName;
		}

		public Builder withCacheReadEnabled(boolean isCacheReadEnabled) {
			if ( isCacheReadEnabled &&
					!persister.getFactory().getSessionFactoryOptions().isSecondLevelCacheEnabled() ) {
				throw new IllegalStateException(
						"isCacheReadEnabled cannot be true when the second-level cache is disabled"
				);
			}
			this.isCacheReadEnabled = isCacheReadEnabled;
			return this;
		}

		public LazyGroupLoader byPrimaryKey() {
			return new LazyGroupLoader(
					persister,
					fetchGroupName,
					isCacheReadEnabled,
					new QueryBuildingParametersImpl(
							LoadQueryInfluencers.NONE,
							1,
							LockMode.NONE,
							null
					)
			);
		}
	}

	private final FetchGroupLoadQueryDetails staticLoadQuery;

	private LazyGroupLoader(
			OuterJoinLoadable persister,
			String fetchGroupName,
			boolean isCacheReadEnabled,
			QueryBuildingParameters buildingParameters) {
		super(
				persister,
				persister.getFactory(),
				new FetchGroupLoadPlanBuildingAssociationVisitationStrategy(
						persister,
						fetchGroupName,
						isCacheReadEnabled,
						persister.getFactory()
				)
		);
		final EntityReturn rootReturn = RootHelper.INSTANCE.extractRootReturn( getLoadPlan(), EntityReturn.class );
		final AliasResolutionContextImpl aliasResolutionContext = new AliasResolutionContextImpl( persister.getFactory() );

		this.staticLoadQuery = new FetchGroupLoadQueryDetails(
				getLoadPlan(),
				persister.getIdentifierColumnNames(),
				aliasResolutionContext,
				rootReturn,
				buildingParameters,
				persister.getFactory(),
				fetchGroupName
		);

		// Should be just one querySpace (of type EntityQuerySpace) in querySpaces.  Should we validate that?
		// Should we make it a util method on Helper like we do for extractRootReturn ?

		log.debugf(
				"Static select for entity %s, fetch group %s: %s",
				persister.getEntityName(),
				fetchGroupName,
				getStaticLoadQuery()
		);
	}

	@Override
	public FetchGroupLoadQueryDetails getLoadQueryDetails() {
		return staticLoadQuery;
	}

	public Object load(
			Serializable id,
			Object optionalObject,
			SharedSessionContractImplementor session) {
		return super.load( id, optionalObject, session, LockOptions.NONE );
	}

	private static class FetchGroupLoadQueryDetails extends EntityLoadQueryDetails {

		/**
		 * Constructs a FetchGroupLoadQueryDetails object from the given inputs.
		 *
		 * @param loadPlan The load plan
		 * @param keyColumnNames The columns to load the entity by (the PK columns or some other unique set of columns)
		 * @param rootReturn that add additional joins here)
		 * @param factory The SessionFactory
		 */
		public FetchGroupLoadQueryDetails(
				LoadPlan loadPlan,
				String[] keyColumnNames,
				AliasResolutionContextImpl aliasResolutionContext,
				EntityReturn rootReturn,
				QueryBuildingParameters buildingParameters,
				SessionFactoryImplementor factory,
				String fetchGroupName) {
			super(
					loadPlan,
					keyColumnNames,
					aliasResolutionContext ,
					rootReturn,
					buildingParameters,
					factory,
					new FetchGroupEntityReturnReader( rootReturn, aliasResolutionContext, fetchGroupName )
			);

			// It would be nice to be able to stash fetchGroupName in this object.
			// The problem is that it is needed by #applyRootReturnSelectFragments
			// while the super constructor is executing. Relying on the stashed
			// value in #applyRootReturnSelectFragments results in a null value.
		}

		protected void applyRootReturnSelectFragments(SelectStatementBuilder selectStatementBuilder) {
			final Queryable queryable = (Queryable) getRootEntityReturn().getEntityPersister();
			final EntityReferenceAliases entityReferenceAliases =
					getAliasResolutionContext().resolveEntityReferenceAliases( getRootEntityReturn().getQuerySpaceUid() );

			//final String identifierFragment = queryable.identifierSelectFragment(
			//		entityReferenceAliases.getTableAlias(),
			//		entityReferenceAliases.getColumnAliases().getSuffix()
			//);
			String propertyFragment = queryable.lazyGroupSelectFragment(
					entityReferenceAliases.getTableAlias(),
					entityReferenceAliases.getColumnAliases().getSuffix(),
					( (FetchGroupEntityReturnReader) getReaderCollector().getReturnReader() ).getFetchGroupName()
			);
			if ( !propertyFragment.isEmpty() ) {
				propertyFragment = propertyFragment.substring( 2 );
			}
			selectStatementBuilder.appendSelectClauseFragment(
				/*identifierFragment + */propertyFragment
			);
		}
	}

	private static class FetchGroupEntityReturnReader extends EntityReturnReader {

		private final AliasResolutionContext aliasResolutionContext;
		private final EntityPersister entityPersister;
		private final String fetchGroupName;
		private final List<LazyAttributeDescriptor> fetchGroupAttributeDescriptors;

		public FetchGroupEntityReturnReader(
				EntityReturn entityReturn,
				AliasResolutionContext aliasResolutionContext,
				String fetchGroupName) {
			super( entityReturn );
			this.aliasResolutionContext = aliasResolutionContext;
			this.entityPersister = entityReturn.getEntityPersister();
			this.fetchGroupName = fetchGroupName;
			this.fetchGroupAttributeDescriptors =
					entityPersister
							.getEntityMetamodel()
							.getBytecodeEnhancementMetadata()
							.getLazyAttributesMetadata()
							.getFetchGroupAttributeDescriptors( fetchGroupName );
		}

		@Override
		public Object read(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
			final Object entity = super.read( resultSet, context );

			final LazyPropertyInitializer.InterceptorImplementor interceptor = ( (PersistentAttributeInterceptable) entity )
					.$$_hibernate_getInterceptor();
			assert interceptor != null : "Expecting bytecode interceptor to be non-null";
			final Set<String> initializedLazyAttributeNames = interceptor.getInitializedLazyAttributeNames();

			final EntityEntry entry = context.getSession().getPersistenceContext().getEntry( entity );
			final Object[] snapshot = entry.getLoadedState();

			final String[][] propertyAliases =
					aliasResolutionContext.resolveEntityReferenceAliases( getEntityReturn().getQuerySpaceUid() )
							.getColumnAliases()
							.getSuffixedPropertyAliases();

			for ( LazyAttributeDescriptor fetchGroupAttributeDescriptor : fetchGroupAttributeDescriptors ) {
				final boolean previousInitialized = initializedLazyAttributeNames.contains(
						fetchGroupAttributeDescriptor.getName()
				);

				if ( previousInitialized ) {
					// todo : one thing we should consider here is potentially un-marking an attribute as dirty based on the selected value
					// 		we know the current value - getPropertyValue( entity, fetchGroupAttributeDescriptor.getAttributeIndex() );
					// 		we know the selected value (see selectedValue below)
					//		we can use the attribute Type to tell us if they are the same
					//
					//		assuming entity is a SelfDirtinessTracker we can also know if the attribute is
					//			currently considered dirty, and if really not dirty we would do the un-marking
					//
					//		of course that would mean a new method on SelfDirtinessTracker to allow un-marking

					// its already been initialized (e.g. by a write) so we don't want to overwrite
					continue;
				}


				final Object selectedValue = fetchGroupAttributeDescriptor.getType().nullSafeGet(
						resultSet,
						propertyAliases[fetchGroupAttributeDescriptor.getAttributeIndex()],
						context.getSession(),
						entity
				);

				entityPersister.setPropertyValue(
						entity,
						fetchGroupAttributeDescriptor.getAttributeIndex(),
						selectedValue
				);
				if ( snapshot != null ) {
					// object have been loaded with setReadOnly(true); HHH-2236
					snapshot[fetchGroupAttributeDescriptor.getAttributeIndex()] =
							fetchGroupAttributeDescriptor.getType().deepCopy(
									selectedValue,
									entityPersister.getFactory()
							);
				}
			}
			return entity;
		}

		public String getFetchGroupName() {
			return fetchGroupName;
		}
	}
}