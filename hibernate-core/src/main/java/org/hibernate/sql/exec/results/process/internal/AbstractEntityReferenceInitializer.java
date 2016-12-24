/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.WrongClassException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.persister.common.spi.Attribute;
import org.hibernate.persister.common.spi.PluralAttribute;
import org.hibernate.persister.common.spi.SingularAttribute;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.convert.results.spi.EntityReference;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.exec.results.process.spi.EntityReferenceInitializer;
import org.hibernate.sql.exec.results.process.spi.InitializerParent;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionGroup;
import org.hibernate.type.CollectionType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityReferenceInitializer
		extends AbstractFetchParentInitializer
		implements EntityReferenceInitializer {

	private final EntityReference entityReference;
	private final boolean isEntityReturn;
	private final Map<Attribute, SqlSelectionGroup> sqlSelectionGroupMap;
	private final boolean isShallow;

	// in-flight processing state
	private Object identifierHydratedState;
	private EntityPersister concretePersister;
	private EntityKey entityKey;
	private Object entityInstance;

	public AbstractEntityReferenceInitializer(
			InitializerParent parent,
			EntityReference entityReference,
			boolean isEntityReturn,
			Map<Attribute, SqlSelectionGroup> sqlSelectionGroupMap,
			boolean isShallow) {
		super( parent );
		this.entityReference = entityReference;
		this.isEntityReturn = isEntityReturn;
		this.sqlSelectionGroupMap = sqlSelectionGroupMap;
		this.isShallow = isShallow;
	}

	@Override
	public EntityReference getEntityReference() {
		return entityReference;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@Override
	public void hydrateIdentifier(RowProcessingState rowProcessingState) {
		if ( identifierHydratedState != null ) {
			// its already been read...
			return;
		}

		identifierHydratedState = buildIdentifierHydratedForm( rowProcessingState );
	}

	private Object buildIdentifierHydratedForm(RowProcessingState rowProcessingState) {
		final SqlSelectionGroup sqlSelectionGroup = sqlSelectionGroupMap.get( entityReference.getEntityPersister().getHierarchy().getIdentifierDescriptor() );

		final int selectionsConsumed = sqlSelectionGroup.getSqlSelections().size();
		if ( selectionsConsumed == 1 ) {
			return rowProcessingState.getJdbcValues()[ sqlSelectionGroup.getSqlSelections().get( 0 ).getValuesArrayPosition() ];
		}
		else {
			final Object[] value = new Object[selectionsConsumed];
			for ( int i = 0; i < selectionsConsumed; i++ ){
				value[i] = rowProcessingState.getJdbcValues()[ sqlSelectionGroup.getSqlSelections().get( i ).getValuesArrayPosition() ];
			}
			return value;
		}
	}

	@Override
	public void resolveEntityKey(RowProcessingState rowProcessingState) {
		if ( entityKey != null ) {
			// its already been resolved
			return;
		}

		if ( identifierHydratedState == null ) {
			throw new ExecutionException( "Entity identifier state not yet hydrated on call to resolve EntityKey" );
		}

		final SharedSessionContractImplementor persistenceContext = rowProcessingState.getJdbcValuesSourceProcessingState().getPersistenceContext();
		concretePersister = resolveConcreteEntityPersister( rowProcessingState, persistenceContext );

		//		1) resolve the value(s) into its identifier representation
		final Object id = concretePersister.getEntityPersister().getIdentifierType().assemble(
				(Serializable) identifierHydratedState,
				persistenceContext,
				null
		);

		//		2) build and register an EntityKey
		this.entityKey = new EntityKey( (Serializable) id, concretePersister.getEntityPersister() );

		//		3) schedule the EntityKey for batch loading, if possible
		if ( shouldBatchFetch() && concretePersister.getEntityPersister().isBatchLoadable() ) {
			if ( !persistenceContext.getPersistenceContext().containsEntity( entityKey ) ) {
				persistenceContext.getPersistenceContext().getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
			}
		}
	}

	/**
	 * Should we consider this entity reference batchable?
	 */
	protected boolean shouldBatchFetch() {
		return true;
	}

	// From CollectionType.
	//		todo : expose CollectionType#NOT_NULL_COLLECTION as public
	private static final Object NOT_NULL_COLLECTION = new MarkerObject( "NOT NULL COLLECTION" );

	@Override
	public void hydrateEntityState(RowProcessingState rowProcessingState) {
		if ( entityInstance != null ) {
			return;
		}

		if ( entityKey == null ) {
			throw new ExecutionException( "EntityKey not yet resolved on call to hydrated entity state" );
		}

		if ( isShallow ) {
			return;
		}

		int numberOfNonIdentifierAttributes = concretePersister.getNonIdentifierAttributes().size();

		final Object rowId;
		if ( concretePersister.getHierarchy().getRowIdDescriptor() != null ) {
			final SqlSelectionGroup sqlSelectionGroup = sqlSelectionGroupMap.get(
					entityReference.getEntityPersister().getHierarchy().getRowIdDescriptor()
			);

			numberOfNonIdentifierAttributes -= 1;
			rowId = rowProcessingState.getJdbcValues()[sqlSelectionGroup.getSqlSelections()
					.get( 0 )
					.getValuesArrayPosition()];

			if ( rowId == null ) {
				throw new HibernateException(
						"Could not read entity row-id from JDBC : " + entityKey
				);
			}
		}
		else {
			rowId = null;
		}

		final Object[] hydratedState = new Object[ numberOfNonIdentifierAttributes ];
		int i = 0;
		for ( Attribute attribute : concretePersister.getNonIdentifierAttributes() ) {
			// todo : need to account for non-eager entities by calling something other than Type#resolve (which loads the entity)
			//		something akin to org.hibernate.persister.entity.AbstractEntityPersister.hydrate() but that operates on Object[], not ResultSet
			//
			//		really at this point any fetches are known which should help - here we'd simply get the instance for that fetch's
			// 		initializer and that fetch's initializer would take care of initializing the state
			//
			//		alternative is something like: AttributeDescriptor#getHydrator#hydrate(Object[] jdbcValues, ...)
			//		and later something like: AttributeDescriptor#getResolver#resolve(Object[] hydratedValues, ...)

			final Object hydratedValue;
			if ( attribute instanceof PluralAttribute ) {
				assert attribute.getOrmType() instanceof CollectionType;
				hydratedValue = NOT_NULL_COLLECTION;
			}
			else {
				SingularAttribute singularAttribute = (SingularAttribute) attribute;
				final SqlSelectionGroup selectionGroup = sqlSelectionGroupMap.get( singularAttribute );
				if ( selectionGroup == null ) {
					// not selected (lazy group, etc)
					hydratedValue = LazyPropertyInitializer.UNFETCHED_PROPERTY;
				}
				else {
					final int numberOfSelections = selectionGroup.getSqlSelections().size();
					if ( numberOfSelections == 1 ) {
						hydratedValue = rowProcessingState.getJdbcValues()[ selectionGroup.getSqlSelections().get( 0 ).getValuesArrayPosition() ];
					}
					else {
						final Object[] sliceValues = new Object[ numberOfSelections ];
						for ( int x = 0; x < numberOfSelections; x++ ) {
							sliceValues[x] = rowProcessingState.getJdbcValues()[ selectionGroup.getSqlSelections().get( x ).getValuesArrayPosition() ];
						}
						hydratedValue = sliceValues;
					}
				}
			}


			hydratedState[i] = hydratedValue;
			i++;
		}

		final SharedSessionContractImplementor persistenceContext = rowProcessingState.getJdbcValuesSourceProcessingState().getPersistenceContext();

		// this isEntityReturn bit is just for entity loaders, not hql/criteria
		if ( isEntityReturn ) {
			final Serializable requestedEntityId = rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions().getEffectiveOptionalId();
			if ( requestedEntityId != null && requestedEntityId.equals( entityKey.getIdentifier() ) ) {
				entityInstance = rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions().getEffectiveOptionalObject();
			}
		}
		if ( entityInstance == null ) {
			entityInstance = persistenceContext.instantiate( concretePersister.getEntityName(), entityKey.getIdentifier() );
		}

		rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingEntity(
				entityKey,
				concretePersister,
				entityInstance,
				hydratedState
		);

		TwoPhaseLoad.postHydrate(
				concretePersister.getEntityPersister(),
				entityKey.getIdentifier(),
				hydratedState,
				// ROW_ID
				null,
				entityInstance,
				// LockMode
				isEntityReturn ? LockMode.READ : LockMode.NONE,
				persistenceContext
		);
	}

	private EntityPersister resolveConcreteEntityPersister(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor persistenceContext) throws WrongClassException {
		final EntityPersister persister = getEntityReference().getEntityPersister();
		if ( persister.getHierarchy().getDiscriminatorDescriptor() == null ) {
			return persister;
		}

		final SqlSelectionGroup selectionGroup = sqlSelectionGroupMap.get( persister.getHierarchy().getDiscriminatorDescriptor() );
		// simple assert here since this should have been validate when building the metamodel
		assert selectionGroup.getSqlSelections().size() == 1;

		final Object discriminatorValue = rowProcessingState.getJdbcValues()[
				selectionGroup.getSqlSelections().get( 0 ).getValuesArrayPosition()
		];

		final Loadable legacyLoadable = (Loadable) persister.getEntityPersister();
		final String result = legacyLoadable.getSubclassForDiscriminatorValue( discriminatorValue );

		if ( result == null ) {
			//woops we got an instance of another class hierarchy branch
			throw new WrongClassException(
					"Discriminator: " + discriminatorValue,
					(Serializable) identifierHydratedState,
					legacyLoadable.getEntityName()
			);
		}


		// Cannot do this until ImprovedEntityPersister is merged into EntityPersister
		// 		todo : enabled this after ImprovedEntityPersister is merged into EntityPersister
		//return persistenceContext.getFactory().getMetamodel().entityPersister( result );
		return persister;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
	}

	private boolean isReadOnly(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor persistenceContext) {
		// todo : need to move #isDefaultReadOnly to SharedSessionContractImplementor
		//return rowProcessingState.getJdbcValuesSourceProcessingState().getQueryOptions().isReadOnly()
		//		|| persistenceContext.isDefaultReadOnly();
		return false;
	}
}
