/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.internal.NoBatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.CompositeGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.builder.AbstractTableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderSkipped;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationOperationGroupFactory;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;
import org.hibernate.tuple.entity.EntityMetamodel;

import static org.hibernate.engine.OptimisticLockStyle.DIRTY;
import static org.hibernate.engine.internal.Versioning.isVersionIncrementRequired;
import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.identifiedResultsCheck;
import static org.hibernate.generator.EventType.UPDATE;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_INT_ARRAY;
import static org.hibernate.internal.util.collections.ArrayHelper.contains;
import static org.hibernate.internal.util.collections.ArrayHelper.join;
import static org.hibernate.internal.util.collections.ArrayHelper.trim;

/**
 * Coordinates the updating of an entity.
 *
 * @see #update
 *
 * @author Steve Ebersole
 */
public class UpdateCoordinatorStandard extends AbstractMutationCoordinator implements UpdateCoordinator {
	// `org.hibernate.orm.test.mapping.onetoone.OneToOneMapsIdChangeParentTest#test` expects
	// the logger-name to be AbstractEntityPersister
	// todo (mutation) : Change this?  It is an interesting "api" question wrt logging
//	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( UpdateCoordinatorStandard.class );
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractEntityPersister.class );

	private final MutationOperationGroup staticUpdateGroup;
	private final BatchKey batchKey;

	private final MutationOperationGroup versionUpdateGroup;
	private final BatchKey versionUpdateBatchkey;

	public UpdateCoordinatorStandard(AbstractEntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );

		// NOTE : even given dynamic-update and/or dirty optimistic locking
		// there are cases where we need the full static updates.
		this.staticUpdateGroup = buildStaticUpdateGroup();
		this.versionUpdateGroup = buildVersionUpdateGroup();
		if ( entityPersister.hasUpdateGeneratedProperties() ) {
			// disable batching in case of update generated properties
			this.batchKey = null;
			this.versionUpdateBatchkey = null;
		}
		else {
			this.batchKey = new BasicBatchKey(
					entityPersister.getEntityName() + "#UPDATE",
					null
			);
			this.versionUpdateBatchkey = new BasicBatchKey(
					entityPersister.getEntityName() + "#UPDATE_VERSION",
					null
			);
		}
	}

	//Used by Hibernate Reactive to efficiently create new instances of this same class
	protected UpdateCoordinatorStandard(
			AbstractEntityPersister entityPersister,
			SessionFactoryImplementor factory,
			MutationOperationGroup staticUpdateGroup,
			BatchKey batchKey,
			MutationOperationGroup versionUpdateGroup,
			BatchKey versionUpdateBatchkey) {
		super( entityPersister, factory );
		this.staticUpdateGroup = staticUpdateGroup;
		this.batchKey = batchKey;
		this.versionUpdateGroup = versionUpdateGroup;
		this.versionUpdateBatchkey = versionUpdateBatchkey;
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return staticUpdateGroup;
	}

	protected MutationOperationGroup getVersionUpdateGroup() {
		return versionUpdateGroup;
	}

	protected BatchKey getBatchKey() {
		return batchKey;
	}

	public final boolean isModifiableEntity(EntityEntry entry) {
		return ( entry == null ? entityPersister().isMutable() : entry.isModifiableEntity() );
	}

	@Override
	public void forceVersionIncrement(
			Object id,
			Object currentVersion,
			Object nextVersion,
			SharedSessionContractImplementor session) {
		if ( versionUpdateGroup == null ) {
			throw new HibernateException( "Cannot force version increment relative to sub-type; use the root type" );
		}
		doVersionUpdate( null, id, nextVersion, currentVersion, session );
	}

	@Override
	public void forceVersionIncrement(
			Object id,
			Object currentVersion,
			Object nextVersion,
			boolean batching,
			SharedSessionContractImplementor session) {
		if ( versionUpdateGroup == null ) {
			throw new HibernateException( "Cannot force version increment relative to sub-type; use the root type" );
		}
		doVersionUpdate( null, id, nextVersion, currentVersion, batching, session );
	}

	@Override
	public GeneratedValues update(
			Object entity,
			Object id,
			Object rowId,
			Object[] values,
			Object oldVersion,
			Object[] incomingOldValues,
			int[] incomingDirtyAttributeIndexes,
			boolean hasDirtyCollection,
			SharedSessionContractImplementor session) {
		final EntityVersionMapping versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping != null ) {
			final Supplier<GeneratedValues> generatedValuesAccess = handlePotentialImplicitForcedVersionIncrement(
					entity,
					id,
					values,
					oldVersion,
					incomingDirtyAttributeIndexes,
					session,
					versionMapping
			);
			if ( generatedValuesAccess != null ) {
				return generatedValuesAccess.get();
			}
		}

		final EntityEntry entry = session.getPersistenceContextInternal().getEntry( entity );

		// Ensure that an immutable or non-modifiable entity is not being updated unless it is
		// in the process of being deleted.
		if ( entry == null && !entityPersister().isMutable() ) {
			throw new IllegalStateException( "Updating immutable entity that is not in session yet" );
		}

		// apply any pre-update in-memory value generation
		final int[] preUpdateGeneratedAttributeIndexes = preUpdateInMemoryValueGeneration( entity, values, session );
		final int[] dirtyAttributeIndexes = dirtyAttributeIndexes( incomingDirtyAttributeIndexes, preUpdateGeneratedAttributeIndexes );

		final boolean[] attributeUpdateability;
		boolean forceDynamicUpdate;

		if ( entityPersister().getEntityMetamodel().isDynamicUpdate() && dirtyAttributeIndexes != null ) {
			attributeUpdateability = getPropertiesToUpdate( dirtyAttributeIndexes, hasDirtyCollection );
			forceDynamicUpdate = true;
		}
		else if ( !isModifiableEntity( entry ) ) {
			// either the entity is mapped as immutable or has been marked as read-only within the Session
			attributeUpdateability = getPropertiesToUpdate(
					dirtyAttributeIndexes == null ? EMPTY_INT_ARRAY : dirtyAttributeIndexes,
					hasDirtyCollection
			);
			forceDynamicUpdate = true;
		}
		else if ( dirtyAttributeIndexes != null
				&& entityPersister().hasUninitializedLazyProperties( entity )
				&& entityPersister().hasLazyDirtyFields( dirtyAttributeIndexes ) ) {
			// we have an entity with dirty lazy attributes.  we need to use dynamic
			// delete and add the dirty, lazy attributes plus the non-lazy attributes
			forceDynamicUpdate = true;
			attributeUpdateability = getPropertiesToUpdate( dirtyAttributeIndexes, hasDirtyCollection );

			final boolean[] propertyLaziness = entityPersister().getPropertyLaziness();
			for ( int i = 0; i < propertyLaziness.length; i++ ) {
				// add also all the non-lazy properties because dynamic update is false
				if ( !propertyLaziness[i] ) {
					attributeUpdateability[i] = true;
				}
			}
		}
		else {
			attributeUpdateability = getPropertyUpdateability( entity );
			forceDynamicUpdate = entityPersister().hasUninitializedLazyProperties( entity );
		}


		return performUpdate(
				entity,
				id,
				rowId,
				values,
				oldVersion,
				incomingOldValues,
				hasDirtyCollection,
				session,
				versionMapping,
				dirtyAttributeIndexes,
				attributeUpdateability,
				forceDynamicUpdate
		);
	}

	protected GeneratedValues performUpdate(
			Object entity,
			Object id,
			Object rowId,
			Object[] values,
			Object oldVersion,
			Object[] incomingOldValues,
			boolean hasDirtyCollection,
			SharedSessionContractImplementor session,
			EntityVersionMapping versionMapping, int[] dirtyAttributeIndexes,
			boolean[] attributeUpdateability,
			boolean forceDynamicUpdate) {

		final InclusionChecker dirtinessChecker =
				(position, attribute) -> isDirty(
						hasDirtyCollection,
						versionMapping,
						dirtyAttributeIndexes,
						attributeUpdateability,
						position,
						attribute,
						entityPersister()
				);

		final InclusionChecker lockingChecker =
				(position, attribute) -> includedInLock(
						versionMapping,
						dirtinessChecker,
						position,
						attribute,
						entityPersister()
				);

		final InclusionChecker inclusionChecker = (position, attribute) -> attributeUpdateability[position];

		final UpdateValuesAnalysisImpl valuesAnalysis = analyzeUpdateValues(
				entity,
				values,
				oldVersion,
				incomingOldValues,
				dirtyAttributeIndexes,
				inclusionChecker,
				lockingChecker,
				dirtinessChecker,
				rowId,
				forceDynamicUpdate,
				session
		);

		//noinspection StatementWithEmptyBody
		if ( valuesAnalysis.tablesNeedingUpdate.isEmpty() && valuesAnalysis.tablesNeedingDynamicUpdate.isEmpty() ) {
			// nothing to do
			return null;
		}
		else if ( valuesAnalysis.needsDynamicUpdate() ) {
			return doDynamicUpdate(
					entity,
					id,
					rowId,
					values,
					incomingOldValues,
					dirtinessChecker,
					valuesAnalysis,
					session
			);
		}
		else {
			return doStaticUpdate(
					entity,
					id,
					rowId,
					values,
					incomingOldValues,
					valuesAnalysis,
					session
			);
		}
	}

	protected static int[] dirtyAttributeIndexes(int[] incomingDirtyIndexes, int[] preUpdateGeneratedIndexes) {
		if ( preUpdateGeneratedIndexes.length == 0 ) {
			return incomingDirtyIndexes;
		}
		else {
			return incomingDirtyIndexes == null
					? preUpdateGeneratedIndexes
					: join( incomingDirtyIndexes, preUpdateGeneratedIndexes );
		}
	}

	private static boolean isDirty(
			boolean hasDirtyCollection,
			EntityVersionMapping versionMapping,
			int[] dirtyAttributeIndexes,
			boolean[] attributeUpdateability,
			int position,
			SingularAttributeMapping attribute,
			EntityPersister persister) {
		if ( !attributeUpdateability[position] ) {
			return false;
		}
		else if ( versionMapping != null
				&& versionMapping.getVersionAttribute() == attribute) {
			return isVersionIncrementRequired(
					dirtyAttributeIndexes,
					hasDirtyCollection,
					persister.getPropertyVersionability()
			);
		}
		else if ( dirtyAttributeIndexes == null ) {
			// we do not know, so assume it is
			return true;
		}
		else {
			return contains( dirtyAttributeIndexes, position );
		}
	}

	private static boolean includedInLock(
			EntityVersionMapping versionMapping,
			InclusionChecker dirtinessChecker,
			int position,
			SingularAttributeMapping attribute,
			EntityPersister persister) {
		switch ( persister.optimisticLockStyle() ) {
			case NONE:
				return false;
			case VERSION:
				return versionMapping != null
						&& versionMapping.getVersionAttribute() == attribute;
//						&& updateableAttributeIndexes[position];
			case ALL:
				return attribute.getAttributeMetadata().isIncludedInOptimisticLocking();
			case DIRTY:
				return attribute.getAttributeMetadata().isIncludedInOptimisticLocking()
						&& dirtinessChecker.include( position, attribute );
			default:
				throw new AssertionFailure( "unknown OptimisticLockStyle" );
		}
	}

	protected Supplier<GeneratedValues> handlePotentialImplicitForcedVersionIncrement(
			Object entity,
			Object id,
			Object[] values,
			Object oldVersion,
			int[] incomingDirtyAttributeIndexes,
			SharedSessionContractImplementor session,
			EntityVersionMapping versionMapping) {
		// handle case where the only value being updated is the version.
		// we handle this case specially from `#coordinateUpdate` to leverage
		// `#doVersionUpdate`
		final boolean isSimpleVersionUpdate;
		final Object newVersion;

		if ( incomingDirtyAttributeIndexes != null ) {
			if ( incomingDirtyAttributeIndexes.length == 1
					&& versionMapping.getVersionAttribute() == entityPersister().getAttributeMapping( incomingDirtyAttributeIndexes[0] ) ) {
				// special case of only the version attribute itself as dirty
				isSimpleVersionUpdate = true;
				newVersion = values[ incomingDirtyAttributeIndexes[0]];
			}
			else if ( incomingDirtyAttributeIndexes.length == 0 && oldVersion != null ) {
				isSimpleVersionUpdate = !versionMapping.areEqual(
						values[ versionMapping.getVersionAttribute().getStateArrayPosition() ],
						oldVersion,
						session
				);
				newVersion = values[ versionMapping.getVersionAttribute().getStateArrayPosition()];
			}
			else {
				isSimpleVersionUpdate = false;
				newVersion = null;
			}
		}
		else {
			isSimpleVersionUpdate = false;
			newVersion = null;
		}

		if ( isSimpleVersionUpdate ) {
			// we have just the version being updated - use the special handling
			assert newVersion != null;
			final GeneratedValues generatedValues = doVersionUpdate( entity, id, newVersion, oldVersion, session );
			return () -> generatedValues;
		}
		else {
			return null;
		}
	}

	private static boolean isValueGenerated(Generator generator) {
		return generator != null
				&& generator.generatesOnUpdate()
				&& generator.generatedOnExecution();
	}

	private static boolean isValueGenerationInSql(Generator generator, Dialect dialect) {
		assert isValueGenerated( generator );
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql(dialect);
	}

	/**
	 * Which properties appear in the SQL update?
	 * (Initialized, updateable ones!)
	 */
	public boolean[] getPropertyUpdateability(Object entity) {
		return entityPersister().hasUninitializedLazyProperties( entity )
				? entityPersister().getNonLazyPropertyUpdateability()
				: entityPersister().getPropertyUpdateability();
	}

	protected GeneratedValues doVersionUpdate(
			Object entity,
			Object id,
			Object version,
			Object oldVersion,
			SharedSessionContractImplementor session) {
		return doVersionUpdate( entity, id, version, oldVersion, true, session );
	}

	protected GeneratedValues doVersionUpdate(
			Object entity,
			Object id,
			Object version,
			Object oldVersion,
			boolean batching,
			SharedSessionContractImplementor session) {
		assert versionUpdateGroup != null;

		final EntityTableMapping mutatingTableDetails = (EntityTableMapping) versionUpdateGroup.getSingleOperation().getTableDetails();

		final MutationExecutor mutationExecutor = updateVersionExecutor( session, versionUpdateGroup, false, batching );

		final EntityVersionMapping versionMapping = entityPersister().getVersionMapping();

		// set the new version
		mutationExecutor.getJdbcValueBindings().bindValue(
				version,
				mutatingTableDetails.getTableName(),
				versionMapping.getSelectionExpression(),
				ParameterUsage.SET
		);

		// restrict the key
		mutatingTableDetails.getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> {
					mutationExecutor.getJdbcValueBindings().bindValue(
							jdbcValue,
							mutatingTableDetails.getTableName(),
							columnMapping.getColumnName(),
							ParameterUsage.RESTRICT
					);
				},
				session
		);

		// restrict the old-version
		mutationExecutor.getJdbcValueBindings().bindValue(
				oldVersion,
				mutatingTableDetails.getTableName(),
				versionMapping.getSelectionExpression(),
				ParameterUsage.RESTRICT
		);

		try {
			return mutationExecutor.execute(
					entity,
					null,
					(tableMapping) -> tableMapping.getTableName().equals( entityPersister().getIdentifierTableName() ),
					(statementDetails, affectedRowCount, batchPosition) -> identifiedResultsCheck(
							statementDetails,
							affectedRowCount,
							batchPosition,
							entityPersister(),
							id,
							factory()
					),
					session
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	private int[] preUpdateInMemoryValueGeneration(
			Object object,
			Object[] newValues,
			SharedSessionContractImplementor session) {
		final EntityMetamodel entityMetamodel = entityPersister().getEntityMetamodel();
		if ( !entityMetamodel.hasPreUpdateGeneratedValues() ) {
			return EMPTY_INT_ARRAY;
		}

		final Generator[] generators = entityMetamodel.getGenerators();
		if ( generators.length != 0 ) {
			final int[] fieldsPreUpdateNeeded = new int[generators.length];
			int count = 0;
			for ( int i = 0; i < generators.length; i++ ) {
				final Generator generator = generators[i];
				if ( generator != null
						&& !generator.generatedOnExecution( object, session )
						&& generator.generatesOnUpdate() ) {
					newValues[i] = ( (BeforeExecutionGenerator) generator ).generate( session, object, newValues[i], UPDATE );
					entityPersister().setPropertyValue( object, i, newValues[i] );
					fieldsPreUpdateNeeded[count++] = i;
				}
			}

			if ( count > 0 ) {
				return trim( fieldsPreUpdateNeeded, count );
			}
		}

		return EMPTY_INT_ARRAY;
	}

	/**
	 * Transform the array of property indexes to an array of booleans for each attribute,
	 * true when the property is dirty
	 */
	protected boolean[] getPropertiesToUpdate(final int[] dirtyProperties, final boolean hasDirtyCollection) {
		final boolean[] updateability = entityPersister().getPropertyUpdateability();
		if ( dirtyProperties == null ) {
			return updateability;
		}
		else {
			final boolean[] propsToUpdate = new boolean[entityPersister().getNumberOfAttributeMappings()];
			for ( int property: dirtyProperties ) {
				if ( updateability[property] ) {
					propsToUpdate[property] = true;
				}
			}
			if ( entityPersister().isVersioned()
					&& entityPersister().getVersionMapping().getVersionAttribute().isUpdateable() ) {
				final int versionAttributeIndex = entityPersister().getVersionMapping()
						.getVersionAttribute()
						.getStateArrayPosition();
				propsToUpdate[versionAttributeIndex] = propsToUpdate[versionAttributeIndex]
						|| isVersionIncrementRequired(
								dirtyProperties,
								hasDirtyCollection,
								entityPersister().getPropertyVersionability()
						);
			}
			return propsToUpdate;
		}
	}

	private UpdateValuesAnalysisImpl analyzeUpdateValues(
			Object entity,
			Object[] values,
			Object oldVersion,
			Object[] oldValues,
			int[] dirtyAttributeIndexes,
			InclusionChecker inclusionChecker,
			InclusionChecker lockingChecker,
			InclusionChecker dirtinessChecker,
			Object rowId,
			boolean forceDynamicUpdate,
			SharedSessionContractImplementor session) {
		final AbstractEntityPersister persister = entityPersister();
		final AttributeMappingsList attributeMappings = persister.getAttributeMappings();

		// NOTE:
		// 		* `dirtyAttributeIndexes == null` means we had no snapshot and couldn't
		// 			get one using select-before-update; never the case for #merge
		//		* `oldValues == null` just means we had no snapshot to begin with - we might
		//			have used select-before-update to get the dirtyAttributeIndexes (again,
		//			never the case for #merge)
		final UpdateValuesAnalysisImpl analysis = new UpdateValuesAnalysisImpl(
				values,
				oldValues,
				dirtyAttributeIndexes,
				dirtinessChecker,
				rowId,
				forceDynamicUpdate
		);

		final boolean[] propertyUpdateability = persister.getPropertyUpdateability();

		for ( int attributeIndex = 0; attributeIndex < attributeMappings.size(); attributeIndex++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( attributeIndex );
			analysis.startingAttribute( attributeMapping );

			try {
				if ( attributeMapping.getJdbcTypeCount() > 0
						&& attributeMapping instanceof SingularAttributeMapping ) {
					SingularAttributeMapping asSingularAttributeMapping = (SingularAttributeMapping) attributeMapping;
					processAttribute(
							entity,
							analysis,
							attributeIndex,
							asSingularAttributeMapping,
							oldVersion,
							oldValues,
							inclusionChecker,
							lockingChecker,
							session
					);

					// In this case we check for exactly DirtynessStatus.DIRTY so to not log warnings when the user didn't get it wrong:
					if ( analysis.currentAttributeAnalysis.getDirtynessStatus() == AttributeAnalysis.DirtynessStatus.DIRTY ) {
						if ( !propertyUpdateability[attributeIndex] ) {
							LOG.ignoreImmutablePropertyModification( attributeMapping.getAttributeName(), persister.getEntityName() );
						}
					}
				}
			}
			finally {
				analysis.finishedAttribute( attributeMapping );
			}
		}

		return analysis;
	}

	private void processAttribute(
			Object entity,
			UpdateValuesAnalysisImpl analysis,
			int attributeIndex,
			SingularAttributeMapping attributeMapping,
			Object oldVersion,
			Object[] oldValues,
			InclusionChecker inclusionChecker,
			InclusionChecker lockingChecker,
			SharedSessionContractImplementor session) {

		final Generator generator = attributeMapping.getGenerator();
		final boolean generated = isValueGenerated( generator );
		final boolean needsDynamicUpdate = generated && session != null && !generator.generatedOnExecution( entity, session );
		final boolean generatedInSql = generated && isValueGenerationInSql( generator, dialect );
		if ( generatedInSql && !needsDynamicUpdate && !( (OnExecutionGenerator) generator ).writePropertyValue() ) {
			analysis.registerValueGeneratedInSqlNoWrite();
		}

		if ( needsDynamicUpdate || generatedInSql || inclusionChecker.include( attributeIndex, attributeMapping ) ) {
			final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
			for ( int i = 0; i < jdbcTypeCount; i++ ) {
				processSet( analysis, attributeMapping.getSelectable( i ), needsDynamicUpdate );
			}
		}

		if ( lockingChecker.include( attributeIndex, attributeMapping ) ) {
			final Object attributeLockValue = attributeLockValue( attributeIndex, attributeMapping, oldVersion, oldValues );
			processLock( analysis, attributeMapping, session, attributeLockValue );
		}
	}

	private Object attributeLockValue(
			int attributeIndex,
			SingularAttributeMapping attributeMapping,
			Object oldVersion,
			Object[] oldValues) {
		if ( entityPersister().getVersionMapping() != null
				&& entityPersister().getVersionMapping().getVersionAttribute() == attributeMapping ) {
			return oldVersion;
		}
		else {
			return oldValues == null ? null : oldValues[attributeIndex];
		}
	}

	private void processSet(UpdateValuesAnalysisImpl analysis, SelectableMapping selectable, boolean needsDynamicUpdate) {
		if ( selectable != null && !selectable.isFormula() && selectable.isUpdateable() ) {
			final EntityTableMapping tableMapping = entityPersister().getPhysicalTableMappingForMutation( selectable );
			analysis.registerColumnSet( tableMapping, selectable.getSelectionExpression(), selectable.getWriteExpression() );
			if ( needsDynamicUpdate ) {
				analysis.getTablesNeedingDynamicUpdate().add( tableMapping );
			}
		}
	}

	private void processLock(
			UpdateValuesAnalysisImpl analysis,
			SingularAttributeMapping attributeMapping,
			SharedSessionContractImplementor session,
			Object attributeLockValue) {
		attributeMapping.decompose(
				attributeLockValue,
				0,
				analysis,
				null,
				(valueIndex, updateAnalysis, noop, jdbcValue, columnMapping) -> {
					if ( !columnMapping.isFormula() ) {
						final EntityTableMapping tableMapping = entityPersister().getPhysicalTableMappingForMutation( columnMapping );
						updateAnalysis.registerColumnOptLock( tableMapping, columnMapping.getSelectionExpression(), jdbcValue );
					}
				},
				session
		);
	}

	protected GeneratedValues doStaticUpdate(
			Object entity,
			Object id,
			Object rowId,
			Object[] values,
			Object[] oldValues,
			UpdateValuesAnalysisImpl valuesAnalysis,
			SharedSessionContractImplementor session) {

		final MutationExecutor mutationExecutor = executor( session, staticUpdateGroup, false );

		decomposeForUpdate(
				id,
				rowId,
				values,
				valuesAnalysis,
				mutationExecutor,
				staticUpdateGroup,
//				(position, attribute) -> valuesAnalysis.getAttributeAnalyses().get( position ).isDirty(),
				(position, attribute) -> AttributeAnalysis.DirtynessStatus.CONSIDER_LIKE_DIRTY,
				session
		);
		bindPartitionColumnValueBindings( oldValues, session, mutationExecutor.getJdbcValueBindings() );

		try {
			return mutationExecutor.execute(
					entity,
					valuesAnalysis,
					valuesAnalysis.tablesNeedingUpdate::contains,
					(statementDetails, affectedRowCount, batchPosition) -> identifiedResultsCheck(
							statementDetails,
							affectedRowCount,
							batchPosition,
							entityPersister(),
							id,
							factory()
					),
					session
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	protected void decomposeForUpdate(
			Object id,
			Object rowId,
			Object[] values,
			UpdateValuesAnalysisImpl valuesAnalysis,
			MutationExecutor mutationExecutor,
			MutationOperationGroup jdbcOperationGroup,
			DirtinessChecker dirtinessChecker,
			SharedSessionContractImplementor session) {
		final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();

		// apply values
		for ( int position = 0; position < jdbcOperationGroup.getNumberOfOperations(); position++ ) {
			final MutationOperation operation = jdbcOperationGroup.getOperation( position );
			final EntityTableMapping tableMapping = (EntityTableMapping) operation.getTableDetails();
			if ( valuesAnalysis.tablesNeedingUpdate.contains( tableMapping ) ) {
				final int[] attributeIndexes = tableMapping.getAttributeIndexes();
				for ( int i = 0; i < attributeIndexes.length; i++ ) {
					decomposeAttributeForUpdate(
							values,
							valuesAnalysis,
							dirtinessChecker,
							session,
							jdbcValueBindings,
							tableMapping,
							attributeIndexes[ i ]
					);
				}
			}
		}

		// apply keys
		for ( int position = 0; position < jdbcOperationGroup.getNumberOfOperations(); position++ ) {
			final MutationOperation operation = jdbcOperationGroup.getOperation( position );
			final EntityTableMapping tableMapping = (EntityTableMapping) operation.getTableDetails();
			breakDownKeyJdbcValues( id, rowId, session, jdbcValueBindings, tableMapping );
		}
	}

	private void decomposeAttributeForUpdate(
			Object[] values,
			UpdateValuesAnalysisImpl valuesAnalysis,
			DirtinessChecker dirtinessChecker,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableMapping,
			int attributeIndex) {
		final AttributeMapping attributeMapping = entityPersister().getAttributeMappings().get( attributeIndex );
		if ( attributeMapping instanceof SingularAttributeMapping ) {
			final AttributeAnalysis attributeAnalysisRef = valuesAnalysis.attributeAnalyses.get( attributeIndex );
			if ( !attributeAnalysisRef.isSkipped() ) {
				final IncludedAttributeAnalysis attributeAnalysis = (IncludedAttributeAnalysis) attributeAnalysisRef;
				if ( attributeAnalysis.includeInSet() ) {
					// apply the new values
					if ( attributeMapping.isEmbeddedAttributeMapping() ) {
						decomposeEmbeddedAttributeMapping(
								values[attributeIndex],
								session,
								jdbcValueBindings,
								tableMapping,
								entityPersister().getEntityMetamodel().isDynamicUpdate() && dirtinessChecker != null,
								dirtinessChecker != null ?
										dirtinessChecker.isDirty( attributeIndex, attributeMapping ).isDirty() :
										true,
								attributeMapping
						);
					}
					else {
						if ( includeInSet( dirtinessChecker, attributeIndex, attributeMapping, attributeAnalysis ) ) {
							decomposeAttributeMapping(
									session,
									jdbcValueBindings,
									tableMapping,
									attributeMapping,
									values[attributeIndex]
							);
						}
					}
				}

				// apply any optimistic locking
				if ( attributeAnalysis.includeInLocking() ) {
					optimisticLock( session, jdbcValueBindings, tableMapping, attributeAnalysis );
				}
			}
		}
	}

	private void decomposeEmbeddedAttributeMapping(
			Object value,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableMapping,
			boolean isDynamicUpdate,
			boolean isDirty,
			AttributeMapping attributeMapping) {
		final CompositeGenerator compositeGenerator = (CompositeGenerator) attributeMapping.getGenerator();
		attributeMapping.decompose(
				value,
				0,
				jdbcValueBindings,
				tableMapping,
				(valueIndex, bindings, table, jdbcValue, jdbcMapping) -> {
					if ( jdbcMapping instanceof EmbeddedAttributeMapping ) {
						decomposeEmbeddedAttributeMapping(
								jdbcValue,
								session,
								jdbcValueBindings,
								tableMapping,
								isDynamicUpdate,
								isDirty,
								(AttributeMapping) jdbcMapping
						);
					}
					else if ( !isDynamicUpdate || ( isDynamicUpdate && isDirty ) ) {
						if ( jdbcMapping instanceof SingularAttributeMapping ) {
							final SingularAttributeMapping singularAttributeMapping = (SingularAttributeMapping) jdbcMapping;
							final Generator generator = compositeGenerator == null ?
									null :
									compositeGenerator.getGenerator( singularAttributeMapping.getStateArrayPosition() );
							final boolean valueGenerated = isValueGenerated( generator );
							if ( valueGenerated ) {
								if ( isValueGenerationInSql( generator, dialect )
										&& ( (OnExecutionGenerator) generator ).writePropertyValue() ) {
									bindings.bindValue(
											jdbcValue,
											table.getTableName(),
											jdbcMapping.getSelectionExpression(),
											ParameterUsage.SET
									);
								}
								else if ( !jdbcMapping.isFormula() && jdbcMapping.isUpdateable() ) {
									bindings.bindValue(
											jdbcValue,
											table.getTableName(),
											jdbcMapping.getSelectionExpression(),
											ParameterUsage.SET
									);
								}
							}
							else {
								decomposeAttributeMapping(
										session,
										bindings,
										tableMapping,
										singularAttributeMapping,
										jdbcValue
								);
							}
						}
						else {
							if ( !jdbcMapping.isFormula() && jdbcMapping.isUpdateable() ) {
								bindings.bindValue(
										jdbcValue,
										table.getTableName(),
										jdbcMapping.getSelectionExpression(),
										ParameterUsage.SET
								);
							}
						}
					}
				},
				session
		);
	}

	private static void optimisticLock(
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableMapping,
			IncludedAttributeAnalysis attributeAnalysis) {
		attributeAnalysis.columnLockingAnalyses.forEach( columnLockingAnalysis -> {
			if ( columnLockingAnalysis.getLockValue() != null ) {
				jdbcValueBindings.bindValue(
						columnLockingAnalysis.getLockValue(),
						tableMapping.getTableName(),
						columnLockingAnalysis.getReadExpression(),
						ParameterUsage.RESTRICT
				);
			}
		} );
	}

	private static void decomposeAttributeMapping(
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableMapping,
			AttributeMapping attributeMapping,
			Object values) {
		attributeMapping.decompose(
				values,
				0,
				jdbcValueBindings,
				tableMapping,
				(valueIndex, bindings, table, jdbcValue, jdbcMapping) -> {
					if ( !jdbcMapping.isFormula() && jdbcMapping.isUpdateable() ) {
						bindings.bindValue(
								jdbcValue,
								table.getTableName(),
								jdbcMapping.getSelectionExpression(),
								ParameterUsage.SET
						);
					}
				},
				session
		);
	}

	private boolean includeInSet(
			DirtinessChecker dirtinessChecker,
			int attributeIndex,
			AttributeMapping attributeMapping,
			IncludedAttributeAnalysis attributeAnalysis) {
		if ( attributeAnalysis.isValueGeneratedInSqlNoWrite() ) {
			// we applied `#getDatabaseGeneratedReferencedColumnValue` earlier
			return false;
		}
		else if ( entityPersister().isVersioned()
				&& entityPersister().getVersionMapping().getVersionAttribute() == attributeMapping) {
			return true;
		}
		else if ( entityPersister().getEntityMetamodel().isDynamicUpdate() && dirtinessChecker != null ) {
			return attributeAnalysis.includeInSet() && dirtinessChecker.isDirty( attributeIndex, attributeMapping ).isDirty();
		}
		else {
			return true;
		}
	}

	protected GeneratedValues doDynamicUpdate(
			Object entity,
			Object id,
			Object rowId,
			Object[] values,
			Object[] oldValues,
			InclusionChecker dirtinessChecker,
			UpdateValuesAnalysisImpl valuesAnalysis,
			SharedSessionContractImplementor session) {
		// Create the JDBC operation descriptors
		final MutationOperationGroup dynamicUpdateGroup = generateDynamicUpdateGroup(
				entity,
				id,
				rowId,
				oldValues,
				valuesAnalysis,
				session
		);

		// and then execute them

		final MutationExecutor mutationExecutor = executor( session, dynamicUpdateGroup, true );

		decomposeForUpdate(
				id,
				rowId,
				values,
				valuesAnalysis,
				mutationExecutor,
				dynamicUpdateGroup,
				(attributeIndex, attribute) -> dirtinessChecker.include( attributeIndex, (SingularAttributeMapping) attribute ) ? AttributeAnalysis.DirtynessStatus.CONSIDER_LIKE_DIRTY : AttributeAnalysis.DirtynessStatus.NOT_DIRTY,
				session
		);
		bindPartitionColumnValueBindings( oldValues, session, mutationExecutor.getJdbcValueBindings() );

		try {
			return mutationExecutor.execute(
					entity,
					valuesAnalysis,
					(tableMapping) -> {
						if ( tableMapping.isOptional()
								&& !valuesAnalysis.tablesWithNonNullValues.contains( tableMapping ) ) {
							// the table is optional, and we have null values for all of its columns
							if ( valuesAnalysis.dirtyAttributeIndexes.length > 0 ) {
								return true;
							}
							return false;
						}
						else {
							return valuesAnalysis.tablesNeedingUpdate.contains( tableMapping );
						}
					},
					(statementDetails, affectedRowCount, batchPosition) -> identifiedResultsCheck(
							statementDetails,
							affectedRowCount,
							batchPosition,
							entityPersister(),
							id,
							factory()
					),
					session
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	private MutationExecutor executor(SharedSessionContractImplementor session, MutationOperationGroup group, boolean dynamicUpdate) {
		return mutationExecutorService
				.createExecutor( resolveBatchKeyAccess( dynamicUpdate, session ), group, session );
	}

	private MutationExecutor updateVersionExecutor(SharedSessionContractImplementor session, MutationOperationGroup group, boolean dynamicUpdate) {
		return mutationExecutorService
				.createExecutor( resolveUpdateVersionBatchKeyAccess( dynamicUpdate, session ), group, session );
	}

	private MutationExecutor updateVersionExecutor(
			SharedSessionContractImplementor session,
			MutationOperationGroup group,
			boolean dynamicUpdate,
			boolean batching) {
		if ( batching ) {
			return updateVersionExecutor(session, group,dynamicUpdate);
		}
		return mutationExecutorService.createExecutor( NoBatchKeyAccess.INSTANCE, group, session );

	}

	protected BatchKeyAccess resolveUpdateVersionBatchKeyAccess(boolean dynamicUpdate, SharedSessionContractImplementor session) {
		if ( !dynamicUpdate
				&& session.getTransactionCoordinator() != null
				&& session.getTransactionCoordinator().isTransactionActive() ) {
			return this::getVersionUpdateBatchkey;
		}

		return NoBatchKeyAccess.INSTANCE;
	}

	//Used by Hibernate Reactive
	protected BatchKey getVersionUpdateBatchkey(){
		return versionUpdateBatchkey;
	}

	protected MutationOperationGroup generateDynamicUpdateGroup(
			Object entity,
			Object id,
			Object rowId,
			Object[] oldValues,
			UpdateValuesAnalysisImpl valuesAnalysis,
			SharedSessionContractImplementor session) {
		final MutationGroupBuilder updateGroupBuilder = new MutationGroupBuilder( MutationType.UPDATE, entityPersister() );

		entityPersister().forEachMutableTable( (tableMapping) -> {
			final MutatingTableReference tableReference = new MutatingTableReference( tableMapping );
			final TableMutationBuilder<?> tableUpdateBuilder;
			//noinspection SuspiciousMethodCalls
			if ( ! valuesAnalysis.tablesNeedingUpdate.contains( tableReference.getTableMapping() ) ) {
				// this table does not need updating
				tableUpdateBuilder = new TableUpdateBuilderSkipped( tableReference );
			}
			else {
				tableUpdateBuilder = createTableUpdateBuilder( tableMapping );
			}
			updateGroupBuilder.addTableDetailsBuilder( tableUpdateBuilder );
		} );

		applyTableUpdateDetails(
				entity,
				rowId,
				updateGroupBuilder,
				oldValues,
				valuesAnalysis,
				(position, attribute) -> valuesAnalysis.getAttributeAnalyses().get( position ).getDirtynessStatus(),
				session
		);

		return createOperationGroup( valuesAnalysis, updateGroupBuilder.buildMutationGroup() );
	}

	private TableMutationBuilder<?> createTableUpdateBuilder(EntityTableMapping tableMapping) {
		final GeneratedValuesMutationDelegate delegate = tableMapping.isIdentifierTable() ?
				entityPersister().getUpdateDelegate() :
				null;
		if ( delegate != null ) {
			return delegate.createTableMutationBuilder( tableMapping.getInsertExpectation(), factory() );
		}
		else {
			return newTableUpdateBuilder( tableMapping );
		}
	}

	protected <O extends MutationOperation> AbstractTableUpdateBuilder<O> newTableUpdateBuilder(EntityTableMapping tableMapping) {
		return new TableUpdateBuilderStandard<>( entityPersister(), tableMapping, factory() );
	}

	private void applyTableUpdateDetails(
			Object entity,
			Object rowId,
			MutationGroupBuilder updateGroupBuilder,
			Object[] oldValues,
			UpdateValuesAnalysisImpl updateValuesAnalysis,
			DirtinessChecker dirtinessChecker,
			SharedSessionContractImplementor session) {
		final EntityVersionMapping versionMapping = entityPersister().getVersionMapping();
		final AttributeMappingsList attributeMappings = entityPersister().getAttributeMappings();
		final boolean[] versionability = entityPersister().getPropertyVersionability();
		final OptimisticLockStyle optimisticLockStyle = entityPersister().optimisticLockStyle();

		updateGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final EntityTableMapping tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();

			final int[] attributeIndexes = tableMapping.getAttributeIndexes();
			for ( int i = 0; i < attributeIndexes.length; i++ ) {
				final int attributeIndex = attributeIndexes[i];

				final AttributeMapping attributeMapping = attributeMappings.get( attributeIndex );
				final AttributeAnalysis attributeAnalysis = updateValuesAnalysis.attributeAnalyses.get( attributeIndex );

				if ( attributeAnalysis.includeInSet() ) {
					assert updateValuesAnalysis.tablesNeedingUpdate.contains( tableMapping ) || updateValuesAnalysis.tablesNeedingDynamicUpdate.contains( tableMapping );
					applyAttributeUpdateDetails(
							entity,
							updateGroupBuilder,
							dirtinessChecker,
							versionMapping,
							attributeIndex,
							attributeMapping,
							(TableUpdateBuilder<?>) builder,
							session
					);
				}

				if ( attributeAnalysis.includeInLocking() ) {
					final boolean includeRestriction = includeInRestriction(
							oldValues,
							dirtinessChecker,
							versionMapping,
							versionability,
							optimisticLockStyle,
							attributeIndex,
							attributeMapping,
							attributeAnalysis
					);

					if ( includeRestriction ) {
						applyAttributeLockingDetails(
								oldValues,
								session,
								attributeIndex,
								attributeMapping,
								(TableUpdateBuilder<?>) builder
						);
					}
				}
			}
		} );

		updateGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final EntityTableMapping tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			final TableUpdateBuilder<?> tableUpdateBuilder = (TableUpdateBuilder<?>) builder;
			applyKeyRestriction( rowId, entityPersister(), tableUpdateBuilder, tableMapping );
			applyPartitionKeyRestriction( tableUpdateBuilder );
		} );
	}

	private void applyPartitionKeyRestriction(TableUpdateBuilder<?> tableUpdateBuilder) {
		final AbstractEntityPersister persister = entityPersister();
		if ( persister.hasPartitionedSelectionMapping() ) {
			final AttributeMappingsList attributeMappings = persister.getAttributeMappings();
			for ( int m = 0; m < attributeMappings.size(); m++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( m );
				final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
				for ( int i = 0; i < jdbcTypeCount; i++ ) {
					final SelectableMapping selectableMapping = attributeMapping.getSelectable( i );
					if ( selectableMapping.isPartitioned() ) {
						tableUpdateBuilder.addKeyRestrictionLeniently( selectableMapping );
					}
				}
			}
		}
	}

	private static void applyAttributeLockingDetails(
			Object[] oldValues,
			SharedSessionContractImplementor session,
			int attributeIndex,
			AttributeMapping attributeMapping,
			TableUpdateBuilder<?> tableUpdateBuilder) {
		if ( oldValues == null ) {
			tableUpdateBuilder.addOptimisticLockRestrictions( attributeMapping );
		}
		else if ( tableUpdateBuilder.getOptimisticLockBindings() != null ) {
			attributeMapping.decompose(
					oldValues[attributeIndex],
					tableUpdateBuilder.getOptimisticLockBindings(),
					session
			);
		}
	}

	private static boolean includeInRestriction(
			Object[] oldValues,
			DirtinessChecker dirtinessChecker,
			EntityVersionMapping versionMapping,
			boolean[] versionability,
			OptimisticLockStyle optimisticLockStyle,
			int attributeIndex,
			AttributeMapping attributeMapping,
			AttributeAnalysis attributeAnalysis) {

		if ( optimisticLockStyle == OptimisticLockStyle.VERSION
				&& versionMapping != null
				&& attributeMapping == versionMapping.getVersionAttribute() ) {
			return true;
		}
		else if ( oldValues == null ) {
			return false;
		}
		else if ( optimisticLockStyle == OptimisticLockStyle.ALL ) {
			return versionability[attributeIndex];
		}
		else if ( optimisticLockStyle == DIRTY ) {
			if ( dirtinessChecker == null ) {
				// this should indicate creation of the "static" update group.
				return false;
			}
			else {
				return versionability[attributeIndex]
					&& attributeAnalysis.includeInLocking()
					&& dirtinessChecker.isDirty( attributeIndex, attributeMapping ).isDirty();
			}
		}
		else {
			return false;
		}
	}

	private void applyAttributeUpdateDetails(
			Object entity,
			MutationGroupBuilder updateGroupBuilder,
			DirtinessChecker dirtinessChecker,
			EntityVersionMapping versionMapping,
			int attributeIndex,
			AttributeMapping attributeMapping,
			TableUpdateBuilder<?> tableUpdateBuilder,
			SharedSessionContractImplementor session) {
		final Generator generator = attributeMapping.getGenerator();
		if ( attributeMapping.isEmbeddedAttributeMapping() ) {
			final CompositeGenerator compositeGenerator = (CompositeGenerator) generator;
			attributeMapping.forEachUpdatable(
					(index, selectable) -> {
						Generator gen = null;
						if ( compositeGenerator != null && selectable instanceof AttributeMapping ) {
							gen = compositeGenerator.getGenerator( ( (AttributeMapping) selectable ).getStateArrayPosition() );
						}
						if ( handleGeneratedValue( entity, session, gen ) ) {
							handleUpdateValueGeneration(
									(AttributeMapping) selectable,
									updateGroupBuilder,
									dirtinessChecker,
									(OnExecutionGenerator) gen
							);
						}
					}
			);
			// todo: this code is ugly, I think it can ve included in the the previous foreach
			final boolean includeInSet = !entityPersister().getEntityMetamodel().isDynamicUpdate()
					|| dirtinessChecker == null
					|| dirtinessChecker.isDirty( attributeIndex, attributeMapping ).isDirty();
			if ( includeInSet ) {
				attributeMapping.forEachUpdatable( (index, selectable) -> {
					Generator gen = null;
					if ( compositeGenerator != null && selectable instanceof AttributeMapping ) {
						gen = compositeGenerator.getGenerator( ( (AttributeMapping) selectable ).getStateArrayPosition() );
					}
					if ( !handleGeneratedValue( entity, session, gen ) ) {
						tableUpdateBuilder.addValueColumn( selectable );
					}
				} );
			}
		}
		else if ( handleGeneratedValue( entity, session, generator ) ) {
			handleUpdateValueGeneration( attributeMapping, updateGroupBuilder, dirtinessChecker, (OnExecutionGenerator) generator );
		}
		else if ( versionMapping != null
				&& versionMapping.getVersionAttribute() == attributeMapping) {
			tableUpdateBuilder.addValueColumn( versionMapping.getVersionAttribute() );
		}
		else {
			final boolean includeInSet = !entityPersister().getEntityMetamodel().isDynamicUpdate()
					|| dirtinessChecker == null
					|| dirtinessChecker.isDirty( attributeIndex, attributeMapping ).isDirty();
			if ( includeInSet ) {
				attributeMapping.forEachUpdatable( tableUpdateBuilder );
			}
		}
	}

	private boolean handleGeneratedValue(Object entity, SharedSessionContractImplementor session, Generator gen) {
		return isValueGenerated( gen )
				&& ( session == null && gen.generatedOnExecution() || gen.generatedOnExecution( entity, session ) )
				&& isValueGenerationInSql( gen, dialect );
	}

	/**
	 * Contains the aggregated analysis of the update values to determine
	 * what SQL UPDATE statement(s) should be used to update the entity
	 * and to drive parameter binding
	 */
	protected class UpdateValuesAnalysisImpl implements UpdateValuesAnalysis {
		private final Object[] values;
		private final int[] dirtyAttributeIndexes;
		private final InclusionChecker dirtinessChecker;

		private final TableSet tablesNeedingUpdate = new TableSet();
		private final TableSet tablesNeedingDynamicUpdate = new TableSet();
		private final TableSet tablesWithNonNullValues = new TableSet();
		private final TableSet tablesWithPreviousNonNullValues = new TableSet();

		private final List<AttributeAnalysis> attributeAnalyses = new ArrayList<>();

		// transient values as we perform the analysis
		private AttributeAnalysisImplementor currentAttributeAnalysis;
		private boolean dirtyChecked = false;
		private boolean nullChecked = false;

		public UpdateValuesAnalysisImpl(
				Object[] values,
				Object[] oldValues,
				int[] dirtyAttributeIndexes,
				InclusionChecker dirtinessChecker,
				Object rowId,
				boolean forceDynamicUpdate) {
			this.values = values;
			this.dirtyAttributeIndexes = dirtyAttributeIndexes;
			this.dirtinessChecker = dirtinessChecker;

			entityPersister().forEachMutableTable( (tableMapping) -> {
				if ( values == null ) {
					tablesWithNonNullValues.add( tableMapping );
				}
				else {
					for ( int i = 0; i < tableMapping.getAttributeIndexes().length; i++ ) {
						final int attributeIndex = tableMapping.getAttributeIndexes()[ i ];
						if ( values[ attributeIndex ] != null ) {
							tablesWithNonNullValues.add( tableMapping );
							break;
						}
					}
				}

				if ( dirtyAttributeIndexes == null && tableMapping.hasColumns() ) {
					tablesNeedingUpdate.add( tableMapping );
				}

				if ( oldValues == null ) {
					tablesWithPreviousNonNullValues.add( tableMapping );
				}
				else {
					for ( int i = 0; i < tableMapping.getAttributeIndexes().length; i++ ) {
						final int attributeIndex = tableMapping.getAttributeIndexes()[ i ];
						if ( oldValues[ attributeIndex ] != null ) {
							tablesWithPreviousNonNullValues.add( tableMapping );
							break;
						}
					}
				}

				if ( tableMapping.getUpdateDetails().getCustomSql() == null ) {
					// we should only dynamically update tables w/o custom update sql
					if ( forceDynamicUpdate ) {
						tablesNeedingDynamicUpdate.add( tableMapping );
					}
					else if ( dirtyAttributeIndexes != null ) {
						if ( entityPersister().getEntityMetamodel().isDynamicUpdate()
								|| entityPersister().optimisticLockStyle() == DIRTY ) {
							tablesNeedingDynamicUpdate.add( tableMapping );
						}
						else if ( rowId == null && needsRowId( entityPersister(), tableMapping ) ) {
							tablesNeedingDynamicUpdate.add( tableMapping );
						}
					}
				}
			} );
		}

		@Override
		public Object[] getValues() {
			return values;
		}

		@Override
		public TableSet getTablesNeedingUpdate() {
			return tablesNeedingUpdate;
		}

		@Override
		public TableSet getTablesWithNonNullValues() {
			return tablesWithNonNullValues;
		}

		@Override
		public TableSet getTablesWithPreviousNonNullValues() {
			return tablesWithPreviousNonNullValues;
		}

		@Override
		public List<AttributeAnalysis> getAttributeAnalyses() {
			return attributeAnalyses;
		}

		/**
		 * Basically, can the ({@linkplain UpdateCoordinatorStandard#staticUpdateGroup static update group}
		 * be used or is a dynamic update needed.
		 */
		public boolean needsDynamicUpdate() {
			return !tablesNeedingDynamicUpdate.isEmpty();
		}

		public TableSet getTablesNeedingDynamicUpdate() {
			return tablesNeedingDynamicUpdate;
		}

		/**
		 * Callback at start of processing an attribute
		 */
		public void startingAttribute(AttributeMapping attribute) {
			if ( attribute.getJdbcTypeCount() < 1 || !( attribute instanceof SingularAttributeMapping ) ) {
				currentAttributeAnalysis = new SkippedAttributeAnalysis( attribute );
			}
			else {
				currentAttributeAnalysis = new IncludedAttributeAnalysis( (SingularAttributeMapping) attribute );
				if ( dirtyAttributeIndexes == null
						|| contains( dirtyAttributeIndexes, attribute.getStateArrayPosition() ) ) {
					currentAttributeAnalysis.markDirty( dirtyAttributeIndexes != null );
				}
			}

			attributeAnalyses.add( currentAttributeAnalysis );
		}

		public void finishedAttribute(AttributeMapping attribute) {
			assert currentAttributeAnalysis.getAttribute() == attribute;
			currentAttributeAnalysis = null;
			dirtyChecked = false;
			nullChecked = false;
		}

		/**
		 * Callback to register the setting of a column value
		 */
		public void registerColumnSet(EntityTableMapping table, String readExpression, String writeExpression) {
			final IncludedAttributeAnalysis includedAttributeAnalysis = (IncludedAttributeAnalysis) currentAttributeAnalysis;
			includedAttributeAnalysis.columnValueAnalyses.add( new ColumnSetAnalysis( readExpression, writeExpression ) );

			if ( !dirtyChecked ) {
				final SingularAttributeMapping attribute = includedAttributeAnalysis.attribute;
				if ( dirtinessChecker.include( attribute.getStateArrayPosition(), attribute ) ) {
					tablesNeedingUpdate.add( table );
				}

				dirtyChecked = true;
			}

			if ( values != null && !nullChecked ) {
				final int attributePosition = currentAttributeAnalysis.getAttribute().getStateArrayPosition();
				if ( values[attributePosition] != null ) {
					tablesWithNonNullValues.add( table );
				}
				nullChecked = true;
			}
		}

		public void registerColumnOptLock(EntityTableMapping table, String readExpression, Object lockValue) {
			final IncludedAttributeAnalysis attributeAnalysis = (IncludedAttributeAnalysis) currentAttributeAnalysis;
			attributeAnalysis.columnLockingAnalyses.add( new ColumnLockingAnalysis( readExpression, lockValue ) );

			if ( dirtyAttributeIndexes != null && lockValue == null ) {
				// we need to use `IS NULL` as opposed to `= ?` w/ NULL
				tablesNeedingDynamicUpdate.add( table );
			}
		}

		public void registerValueGeneratedInSqlNoWrite() {
			final IncludedAttributeAnalysis attributeAnalysis = (IncludedAttributeAnalysis) currentAttributeAnalysis;
			attributeAnalysis.setValueGeneratedInSqlNoWrite( true );
		}
	}

	/**
	 * Local extension to AttributeAnalysis
	 */
	private interface AttributeAnalysisImplementor extends AttributeAnalysis {
		/**
		 * @param asCertain set to true when we're sure, false when we merely need to treat the attribute
		 * as dirty but couldn't actually run the comparison.
		 * Once it's marked at least once "with certainty", there is no option to revert to a lower state.
		 */
		void markDirty(boolean asCertain);
	}

	/**
	 * Local AttributeAnalysis implementation for use when the attribute is
	 * to be completely skipped.  Avoids having to define the collections
	 * needed to fully implement AttributeAnalysis.
	 *
	 * @see IncludedAttributeAnalysis
	 */
	private static class SkippedAttributeAnalysis implements AttributeAnalysisImplementor {
		private final AttributeMapping attributeMapping;

		public SkippedAttributeAnalysis(AttributeMapping attributeMapping) {
			this.attributeMapping = attributeMapping;
		}

		@Override
		public AttributeMapping getAttribute() {
			return attributeMapping;
		}

		@Override
		public boolean includeInSet() {
			return false;
		}

		@Override
		public boolean includeInLocking() {
			return false;
		}

		@Override
		public DirtynessStatus getDirtynessStatus() {
			return DirtynessStatus.NOT_DIRTY;
		}

		@Override
		public void markDirty(boolean certainty) {
		}

		@Override
		public String toString() {
			return String.format(
					Locale.ROOT,
					"SkippedAttributeAnalysis(`%s`)",
					attributeMapping.getNavigableRole().getFullPath()
			);
		}
	}

	/**
	 * Local AttributeAnalysis implementation
	 */
	private static class IncludedAttributeAnalysis implements AttributeAnalysisImplementor {
		private final SingularAttributeMapping attribute;

		private final List<ColumnSetAnalysis> columnValueAnalyses;
		private final List<ColumnLockingAnalysis> columnLockingAnalyses;

		private DirtynessStatus dirty = DirtynessStatus.NOT_DIRTY;
		private boolean valueGeneratedInSqlNoWrite;

		public IncludedAttributeAnalysis(SingularAttributeMapping attribute) {
			this.attribute = attribute;

			this.columnValueAnalyses = CollectionHelper.arrayList( attribute.getJdbcTypeCount() );
			this.columnLockingAnalyses = CollectionHelper.arrayList( attribute.getJdbcTypeCount() );
		}

		@Override
		public SingularAttributeMapping getAttribute() {
			return attribute;
		}

		@Override
		public boolean includeInSet() {
			return !columnValueAnalyses.isEmpty();
		}

		@Override
		public boolean includeInLocking() {
			return !columnLockingAnalyses.isEmpty();
		}

		@Override
		public DirtynessStatus getDirtynessStatus() {
			return dirty;
		}

		@Internal
		@Override
		public void markDirty(boolean certain) {
			if ( certain ) {
				this.dirty = DirtynessStatus.DIRTY;
			}
			else if ( this.dirty == DirtynessStatus.NOT_DIRTY ) {
				this.dirty = DirtynessStatus.CONSIDER_LIKE_DIRTY;
			}
		}

		public boolean isValueGeneratedInSqlNoWrite() {
			return valueGeneratedInSqlNoWrite;
		}

		public void setValueGeneratedInSqlNoWrite(boolean valueGeneratedInSqlNoWrite) {
			this.valueGeneratedInSqlNoWrite = valueGeneratedInSqlNoWrite;
		}

		@Override
		public String toString() {
			return String.format(
					Locale.ROOT,
					"IncludedAttributeAnalysis(`%s`)",
					attribute.getNavigableRole().getFullPath()
			);
		}
	}

	private static class ColumnSetAnalysis {
		private final String readExpression;
		private final String writeExpression;

		public ColumnSetAnalysis(String readExpression, String writeExpression) {
			this.readExpression = readExpression;
			this.writeExpression = writeExpression;
		}

		@SuppressWarnings("unused")
		public String getReadExpression() {
			return readExpression;
		}

		@SuppressWarnings("unused")
		public String getWriteExpression() {
			return writeExpression;
		}
	}

	private static class ColumnLockingAnalysis {
		private final String readExpression;
		private final Object lockValue;

		public ColumnLockingAnalysis(String readExpression, Object lockValue) {
			assert readExpression != null;
			assert !readExpression.equals( "?" );

			this.readExpression = readExpression;
			this.lockValue = lockValue;
		}

		public String getReadExpression() {
			return readExpression;
		}

		public Object getLockValue() {
			return lockValue;
		}
	}

	private MutationOperationGroup buildStaticUpdateGroup() {
		final UpdateValuesAnalysisImpl valuesAnalysis = analyzeUpdateValues(
				null,
				null,
				null,
				null,
				null,
				(index,attribute) -> isValueGenerated( attribute.getGenerator() ) && isValueGenerationInSql( attribute.getGenerator(), dialect() )
						|| entityPersister().getPropertyUpdateability()[index],
				(index,attribute) -> {
					switch ( entityPersister().optimisticLockStyle() ) {
						case ALL:
							return true;
						case VERSION:
							final EntityVersionMapping versionMapping = entityPersister().getVersionMapping();
							return versionMapping != null && attribute == versionMapping.getVersionAttribute();
						default:
							return false;
					}
				},
				(index,attribute) -> true,
				"", // pass anything here to generate the row id restriction if possible
				false,
				null
		);

		final MutationGroupBuilder updateGroupBuilder = new MutationGroupBuilder( MutationType.UPDATE, entityPersister() );

		entityPersister().forEachMutableTable( (tableMapping) -> {
			// NOTE : TableUpdateBuilderStandard handles custom sql-update mappings
			updateGroupBuilder.addTableDetailsBuilder( createTableUpdateBuilder( tableMapping ) );
		} );

		// next, iterate each attribute and build the SET and WHERE clauses
		applyTableUpdateDetails(
				null,
				// row-id
				"", // pass anything here to generate the row id restriction if possible
				// the "collector"
				updateGroupBuilder,
				// oldValues
				null,
				valuesAnalysis,
				(position, attribute) -> valuesAnalysis.getAttributeAnalyses().get( position ).getDirtynessStatus(),
				// session
				null
		);

		// build the mutation-group (SQL AST) and convert it into a jdbc-operations (SQL String, etc) group
		return createOperationGroup( valuesAnalysis, updateGroupBuilder.buildMutationGroup() );
	}

	private MutationOperationGroup buildVersionUpdateGroup() {
		final EntityVersionMapping versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping == null ) {
			return null;
		}
		else {
			final EntityTableMapping identifierTableMapping = entityPersister().getIdentifierTableMapping();
			final AbstractTableUpdateBuilder<JdbcMutationOperation> updateBuilder =
					newTableUpdateBuilder( identifierTableMapping );

			updateBuilder.setSqlComment( "forced version increment for " + entityPersister().getRolePath() );

			updateBuilder.addValueColumn( versionMapping );

			updateBuilder.addKeyRestrictionsLeniently( identifierTableMapping.getKeyMapping() );

			updateBuilder.addOptimisticLockRestriction( versionMapping );
			applyPartitionKeyRestriction( updateBuilder );

			//noinspection resource
			final JdbcMutationOperation jdbcMutation = factory()
					.getJdbcServices()
					.getJdbcEnvironment()
					.getSqlAstTranslatorFactory()
					.buildModelMutationTranslator( updateBuilder.buildMutation(), factory() )
					.translate( null, MutationQueryOptions.INSTANCE );

			return MutationOperationGroupFactory.singleOperation( MutationType.UPDATE, entityPersister(), jdbcMutation );
		}
	}

	@FunctionalInterface
	protected interface InclusionChecker {
		boolean include(int position, SingularAttributeMapping attribute);
	}

	@FunctionalInterface
	protected interface DirtinessChecker {
		AttributeAnalysis.DirtynessStatus isDirty(int position, AttributeMapping attribute);
	}

	@Override
	public String toString() {
		return "UpdateCoordinatorStandard(" + entityPersister().getEntityName() + ")";
	}
}
