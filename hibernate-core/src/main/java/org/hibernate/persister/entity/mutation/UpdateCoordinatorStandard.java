/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderSkipped;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationOperationGroupSingle;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;
import org.hibernate.generator.Generator;
import org.hibernate.generator.InDatabaseGenerator;
import org.hibernate.generator.InMemoryGenerator;
import org.hibernate.tuple.entity.EntityMetamodel;

import static org.hibernate.engine.OptimisticLockStyle.ALL;
import static org.hibernate.engine.OptimisticLockStyle.DIRTY;
import static org.hibernate.engine.OptimisticLockStyle.NONE;
import static org.hibernate.engine.OptimisticLockStyle.VERSION;
import static org.hibernate.engine.internal.Versioning.isVersionIncrementRequired;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_INT_ARRAY;

/**
 * Coordinates the updating of an entity.
 *
 * @see #coordinateUpdate
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

	public UpdateCoordinatorStandard(AbstractEntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );

		// NOTE : even given dynamic-update and/or dirty optimistic locking
		// there are cases where we need the full static updates.
		this.staticUpdateGroup = buildStaticUpdateGroup();
		this.versionUpdateGroup = buildVersionUpdateGroup();
		this.batchKey = new BasicBatchKey(
				entityPersister.getEntityName() + "#UPDATE",
				null
		);
	}

	@Override
	public MutationOperationGroup getStaticUpdateGroup() {
		return staticUpdateGroup;
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
	public void coordinateUpdate(
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
			final boolean isForcedVersionIncrement = handlePotentialImplicitForcedVersionIncrement( entity, id, values, oldVersion, incomingDirtyAttributeIndexes, session, versionMapping );
			if ( isForcedVersionIncrement ) {
				return;
			}
		}

		final EntityEntry entry = session.getPersistenceContextInternal().getEntry( entity );

		// Ensure that an immutable or non-modifiable entity is not being updated unless it is
		// in the process of being deleted.
		if ( entry == null && !entityPersister().isMutable() ) {
			throw new IllegalStateException( "Updating immutable entity that is not in session yet" );
		}

		// apply any pre-update in-memory value generation
		final int[] inMemoryGeneratedAttributeIndexes = preUpdateInMemoryValueGeneration(
				entity,
				values,
				session
		);

		final int[] dirtyAttributeIndexes;
		if ( inMemoryGeneratedAttributeIndexes.length > 0 ) {
			if ( incomingDirtyAttributeIndexes == null ) {
				dirtyAttributeIndexes = inMemoryGeneratedAttributeIndexes;
			}
			else {
				dirtyAttributeIndexes = ArrayHelper.join( incomingDirtyAttributeIndexes, inMemoryGeneratedAttributeIndexes );
			}
		}
		else {
			dirtyAttributeIndexes = incomingDirtyAttributeIndexes;
		}

		final boolean[] attributeUpdateability;
		boolean forceDynamicUpdate = false;


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
			if ( entityPersister().hasUninitializedLazyProperties( entity ) ) {
				forceDynamicUpdate = true;
			}
		}


		final InclusionChecker updateabilityChecker =
				(position, attribute) -> isValueGenerationInSql( attribute.getGenerator(), dialect() )
						|| attributeUpdateability[ position ];

		final InclusionChecker dirtinessChecker = (position, attribute) -> {
			if ( !attributeUpdateability[ position ] ) {
				return false;
			}

			if ( versionMapping != null
					&& versionMapping.getVersionAttribute() == attribute ) {
				return isVersionIncrementRequired(
						dirtyAttributeIndexes,
						hasDirtyCollection,
						entityPersister().getPropertyVersionability()
				);
			}

			if ( dirtyAttributeIndexes == null ) {
				// we do not know, so assume it is
				return true;
			}

			return ArrayHelper.contains( dirtyAttributeIndexes, position );
		};

		final InclusionChecker lockingChecker = (position, attribute) -> {
			final OptimisticLockStyle optimisticLockStyle = entityPersister().optimisticLockStyle();
			if ( optimisticLockStyle == NONE ) {
				return false;
			}

			if ( optimisticLockStyle == VERSION ) {
				return versionMapping != null
						&& versionMapping.getVersionAttribute() == attribute;
//						&& updateableAttributeIndexes[position];
			}

			final boolean includeInLocking = attribute.getAttributeMetadataAccess()
					.resolveAttributeMetadata( null )
					.isIncludedInOptimisticLocking();
			if ( !includeInLocking ) {
				return false;
			}

			if ( optimisticLockStyle == ALL ) {
				return true;
			}

			assert optimisticLockStyle == DIRTY;
			return dirtinessChecker.include( position, attribute );
		};

		final UpdateValuesAnalysisImpl valuesAnalysis = analyzeUpdateValues(
				values,
				oldVersion,
				incomingOldValues,
				dirtyAttributeIndexes,
				updateabilityChecker,
				lockingChecker,
				dirtinessChecker,
				forceDynamicUpdate,
				session
		);

		//noinspection StatementWithEmptyBody
		if ( valuesAnalysis.tablesNeedingUpdate.isEmpty() ) {
			// nothing to do
		}
		else if ( valuesAnalysis.needsDynamicUpdate() ) {
			doDynamicUpdate(
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
			doStaticUpdate(
					entity,
					id,
					rowId,
					values,
					valuesAnalysis,
					session
			);
		}
	}

	private boolean handlePotentialImplicitForcedVersionIncrement(
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
		boolean isSimpleVersionUpdate = false;
		Object newVersion = null;

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
		}

		if ( isSimpleVersionUpdate ) {
			// we have just the version being updated - use the special handling
			assert newVersion != null;
			doVersionUpdate( entity, id, newVersion, oldVersion, session );
			return true;
		}
		return false;
	}

	private boolean isValueGenerationInSql(Generator generator, Dialect dialect) {
		return generator != null
			&& generator.generatesOnUpdate()
			&& generator.generatedByDatabase()
			&& ((InDatabaseGenerator) generator).referenceColumnsInSql(dialect);
	}

	private boolean isValueGenerationInSqlNoWrite(Generator generator, Dialect dialect) {
		return generator != null
			&& generator.generatesOnUpdate()
			&& generator.generatedByDatabase()
			&& ((InDatabaseGenerator) generator).referenceColumnsInSql(dialect)
			&& !((InDatabaseGenerator) generator).writePropertyValue();
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

	private void doVersionUpdate(
			Object entity,
			Object id,
			Object version,
			Object oldVersion,
			SharedSessionContractImplementor session) {
		assert versionUpdateGroup != null;

		final EntityTableMapping mutatingTableDetails = (EntityTableMapping) versionUpdateGroup.getSingleOperation().getTableDetails();

		final MutationExecutorService mutationExecutorService = session.getSessionFactory()
				.getServiceRegistry()
				.getService( MutationExecutorService.class );

		final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
				() -> batchKey,
				versionUpdateGroup,
				session
		);

		final EntityVersionMapping versionMapping = entityPersister().getVersionMapping();

		// set the new version
		mutationExecutor.getJdbcValueBindings().bindValue(
				version,
				mutatingTableDetails.getTableName(),
				versionMapping.getSelectionExpression(),
				ParameterUsage.SET,
				session
		);

		// restrict the key
		mutatingTableDetails.getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> mutationExecutor.getJdbcValueBindings().bindValue(
						jdbcValue,
						mutatingTableDetails.getTableName(),
						columnMapping.getSelectionExpression(),
						ParameterUsage.RESTRICT,
						session
				),
				session
		);

		// restrict the old-version
		mutationExecutor.getJdbcValueBindings().bindValue(
				oldVersion,
				mutatingTableDetails.getTableName(),
				versionMapping.getSelectionExpression(),
				ParameterUsage.RESTRICT,
				session
		);

		try {
			mutationExecutor.execute(
					entity,
					null,
					(tableMapping) -> tableMapping.getTableName().equals( entityPersister().getIdentifierTableName() ),
					(statementDetails, affectedRowCount, batchPosition) -> ModelMutationHelper.identifiedResultsCheck(
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
				Generator generator = generators[i];
				if ( generator != null
						&& !generator.generatedByDatabase()
						&& generator.generatesOnUpdate() ) {
					newValues[i] = ( (InMemoryGenerator) generator ).generate( session, object, newValues[i] );
					entityPersister().setPropertyValue( object, i, newValues[i] );
					fieldsPreUpdateNeeded[count++] = i;
				}
			}

			if ( count > 0 ) {
				return ArrayHelper.trim( fieldsPreUpdateNeeded, count );
			}
		}

		return EMPTY_INT_ARRAY;
	}

	/**
	 * Transform the array of property indexes to an array of booleans for each attribute,
	 * true when the property is dirty
	 */
	private boolean[] getPropertiesToUpdate(final int[] dirtyProperties, final boolean hasDirtyCollection) {
		final boolean[] updateability = entityPersister().getPropertyUpdateability();

		if ( dirtyProperties == null ) {
			return updateability;
		}

		final boolean[] propsToUpdate = new boolean[entityPersister().getNumberOfAttributeMappings()];

		for ( int property: dirtyProperties ) {
			if ( updateability[property] ) {
				propsToUpdate[property] = true;
			}
		}

		if ( entityPersister().isVersioned() && entityPersister().getVersionMapping().getVersionAttribute().isUpdateable() ) {
			final int versionAttributeIndex = entityPersister().getVersionMapping()
					.getVersionAttribute()
					.getStateArrayPosition();
			propsToUpdate[versionAttributeIndex] = propsToUpdate[versionAttributeIndex] || isVersionIncrementRequired(
					dirtyProperties,
					hasDirtyCollection,
					entityPersister().getPropertyVersionability()
			);
		}

		return propsToUpdate;
	}

	private UpdateValuesAnalysisImpl analyzeUpdateValues(
			Object[] values,
			Object oldVersion,
			Object[] oldValues,
			int[] dirtyAttributeIndexes,
			InclusionChecker inclusionChecker,
			InclusionChecker lockingChecker,
			InclusionChecker dirtinessChecker,
			boolean forceDynamicUpdate,
			SharedSessionContractImplementor session) {
		final List<AttributeMapping> attributeMappings = entityPersister().getAttributeMappings();

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
				forceDynamicUpdate
		);

		for ( int attributeIndex = 0; attributeIndex < attributeMappings.size(); attributeIndex++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( attributeIndex );
			analysis.startingAttribute( attributeMapping );

			try {
				if ( attributeMapping.getJdbcTypeCount() < 1 ) {
					continue;
				}

				if ( !( attributeMapping instanceof SingularAttributeMapping ) ) {
					continue;
				}

				if ( ! entityPersister().getPropertyUpdateability()[attributeIndex] ) {
					LOG.ignoreImmutablePropertyModification( attributeMapping.getAttributeName(), entityPersister().getEntityName() );
				}

				processAttribute(
						analysis,
						attributeIndex,
						(SingularAttributeMapping) attributeMapping,
						oldVersion,
						oldValues,
						inclusionChecker,
						lockingChecker,
						session
				);
			}
			finally {
				analysis.finishedAttribute( attributeMapping );
			}
		}

		return analysis;
	}

	private void processAttribute(
			UpdateValuesAnalysisImpl analysis,
			int attributeIndex,
			SingularAttributeMapping attributeMapping,
			Object oldVersion,
			Object[] oldValues,
			InclusionChecker inclusionChecker,
			InclusionChecker lockingChecker,
			SharedSessionContractImplementor session) {
		final boolean includeInSet = inclusionChecker.include( attributeIndex, attributeMapping );
		final boolean includeInLock = lockingChecker.include( attributeIndex, attributeMapping );

		if ( includeInSet ) {
			attributeMapping.forEachSelectable( (selectableIndex, selectable) -> {
				if ( selectable.isFormula() ) {
					return;
				}

				if ( !selectable.isUpdateable() ) {
					return;
				}

				final EntityTableMapping tableMapping = entityPersister().getPhysicalTableMappingForMutation( selectable );
				analysis.registerColumnSet( tableMapping, selectable.getSelectionExpression(), selectable.getWriteExpression() );
			} );
		}

		if ( includeInLock ) {
			final Object attributeLockValue;
			if ( entityPersister().getVersionMapping() != null
					&& entityPersister().getVersionMapping().getVersionAttribute() == attributeMapping ) {
				attributeLockValue = oldVersion;
			}
			else {
				attributeLockValue = oldValues == null ? null : oldValues[ attributeIndex ];
			}

			attributeMapping.decompose(
					attributeLockValue,
					(jdbcValue, columnMapping) -> {
						if ( ! columnMapping.isFormula() ) {
							final EntityTableMapping tableMapping = entityPersister().getPhysicalTableMappingForMutation( columnMapping );
							analysis.registerColumnOptLock( tableMapping, columnMapping.getSelectionExpression(), jdbcValue );
						}
					},
					session
			);
		}
	}

	private void doStaticUpdate(
			Object entity,
			Object id,
			Object rowId,
			Object[] values,
			UpdateValuesAnalysisImpl valuesAnalysis,
			SharedSessionContractImplementor session) {
		final MutationExecutorService mutationExecutorService = session.getSessionFactory()
				.getServiceRegistry()
				.getService( MutationExecutorService.class );

		final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
				() -> batchKey,
				staticUpdateGroup,
				session
		);

		decomposeForUpdate(
				id,
				rowId,
				values,
				valuesAnalysis,
				mutationExecutor,
				staticUpdateGroup,
//				(position, attribute) -> valuesAnalysis.getAttributeAnalyses().get( position ).isDirty(),
				(position, attribute) -> true,
				session
		);

		try {
			//noinspection SuspiciousMethodCalls
			mutationExecutor.execute(
					entity,
					valuesAnalysis,
					valuesAnalysis.tablesNeedingUpdate::contains,
					(statementDetails, affectedRowCount, batchPosition) -> ModelMutationHelper.identifiedResultsCheck(
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

	private void decomposeForUpdate(
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
		jdbcOperationGroup.forEachOperation( (position, operation) -> {
			final EntityTableMapping tableMapping = (EntityTableMapping) operation.getTableDetails();
			if ( !valuesAnalysis.tablesNeedingUpdate.contains( tableMapping ) ) {
				return;
			}

			final int[] attributeIndexes = tableMapping.getAttributeIndexes();
			for ( int i = 0; i < attributeIndexes.length; i++ ) {
				final int attributeIndex = attributeIndexes[ i ];
				final AttributeMapping attributeMapping = entityPersister().getAttributeMappings().get( attributeIndex );
				if ( !( attributeMapping instanceof SingularAttributeMapping ) ) {
					continue;
				}

				final AttributeAnalysis attributeAnalysisRef = valuesAnalysis.attributeAnalyses.get( attributeIndex );
				if ( attributeAnalysisRef.isSkipped() ) {
					continue;
				}

				final IncludedAttributeAnalysis attributeAnalysis = (IncludedAttributeAnalysis) attributeAnalysisRef;

				if ( attributeAnalysis.includeInSet() ) {
					// apply the new values
					final boolean includeInSet;

					if ( isValueGenerationInSqlNoWrite( attributeMapping.getGenerator(), dialect() ) ) {
						// we applied `#getDatabaseGeneratedReferencedColumnValue` earlier
						includeInSet = false;
					}
					else if ( entityPersister().isVersioned()
							&& entityPersister().getVersionMapping().getVersionAttribute() == attributeMapping ) {
						includeInSet = true;
					}
					else if ( entityPersister().getEntityMetamodel().isDynamicUpdate() && dirtinessChecker != null ) {
						includeInSet = attributeAnalysis.includeInSet()
								&& dirtinessChecker.isDirty( attributeIndex, attributeMapping );
					}
					else {
						includeInSet = true;
					}

					if ( includeInSet ) {
						attributeMapping.decompose(
								values[ attributeIndex ],
								(jdbcValue, jdbcMapping) -> {
									if ( jdbcMapping.isFormula() ) {
										return;
									}

									if ( !jdbcMapping.isUpdateable() ) {
										return;
									}

									jdbcValueBindings.bindValue(
											jdbcValue,
											tableMapping.getTableName(),
											jdbcMapping.getSelectionExpression(),
											ParameterUsage.SET,
											session
									);
								},
								session
						);
					}
				}

				// apply any optimistic locking
				if ( attributeAnalysis.includeInLocking() ) {
					attributeAnalysis.columnLockingAnalyses.forEach( (columnLockingAnalysis) -> {
						if ( columnLockingAnalysis.getLockValue() != null ) {
							jdbcValueBindings.bindValue(
									columnLockingAnalysis.getLockValue(),
									tableMapping.getTableName(),
									columnLockingAnalysis.getReadExpression(),
									ParameterUsage.RESTRICT,
									session
							);
						}
					} );
				}
			}
		} );

		// apply keys
		jdbcOperationGroup.forEachOperation( (position, operation) -> {
			final EntityTableMapping tableMapping = (EntityTableMapping) operation.getTableDetails();

			// if the mutation is against the identifier table and we need to use row-id...
			if ( tableMapping.isIdentifierTable() && entityPersister().hasRowId() && rowId != null ) {
				// todo (mutation) : make sure the SQL uses row-id in this case
				jdbcValueBindings.bindValue(
						rowId,
						tableMapping.getTableName(),
						entityPersister().getRowIdMapping().getRowIdName(),
						ParameterUsage.RESTRICT,
						session
				);
			}
			else {
				tableMapping.getKeyMapping().breakDownKeyJdbcValues(
						id,
						(jdbcValue, columnMapping) -> jdbcValueBindings.bindValue(
								jdbcValue,
								tableMapping.getTableName(),
								columnMapping.getColumnName(),
								ParameterUsage.RESTRICT,
								session
						),
						session
				);
			}
		} );
	}

	private void doDynamicUpdate(
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
				id,
				rowId,
				oldValues,
				valuesAnalysis,
				session
		);

		// and then execute them
		final MutationExecutorService mutationExecutorService = session.getSessionFactory()
				.getServiceRegistry()
				.getService( MutationExecutorService.class );

		final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
				() -> batchKey,
				dynamicUpdateGroup,
				session
		);

		decomposeForUpdate(
				id,
				rowId,
				values,
				valuesAnalysis,
				mutationExecutor,
				dynamicUpdateGroup,
				(attributeIndex, attribute) -> dirtinessChecker.include( attributeIndex, (SingularAttributeMapping) attribute ),
				session
		);

		try {
			mutationExecutor.execute(
					entity,
					valuesAnalysis,
					(tableMapping) -> {
						//noinspection SuspiciousMethodCalls
						if ( tableMapping.isOptional()
								&& !valuesAnalysis.tablesWithNonNullValues.contains( tableMapping ) ) {
							// the table is optional, and we have null values for all of its columns
							// todo (6.0) : technically we might need to delete row here
							return false;
						}

						//noinspection SuspiciousMethodCalls,RedundantIfStatement
						if ( !valuesAnalysis.tablesNeedingUpdate.contains( tableMapping ) ) {
							// nothing changed
							return false;
						}

						return true;
					},
					(statementDetails, affectedRowCount, batchPosition) -> ModelMutationHelper.identifiedResultsCheck(
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

	private MutationOperationGroup generateDynamicUpdateGroup(
			Object id,
			Object rowId,
			Object[] oldValues,
			UpdateValuesAnalysisImpl valuesAnalysis,
			SharedSessionContractImplementor session) {
		final MutationGroupBuilder updateGroupBuilder = new MutationGroupBuilder( MutationType.UPDATE, entityPersister() );

		entityPersister().forEachMutableTable( (tableMapping) -> {
			final RestrictedTableMutationBuilder<?,?> tableUpdateBuilder;

			final MutatingTableReference tableReference = new MutatingTableReference( tableMapping );
			//noinspection SuspiciousMethodCalls
			if ( ! valuesAnalysis.tablesNeedingUpdate.contains( tableReference.getTableMapping() ) ) {
				// this table does not need updating
				tableUpdateBuilder = new TableUpdateBuilderSkipped( tableReference );
			}
			else {
				tableUpdateBuilder = new TableUpdateBuilderStandard<>(
						entityPersister(),
						tableMapping,
						factory()
				);
			}
			updateGroupBuilder.addTableDetailsBuilder( tableUpdateBuilder );
		} );

		applyTableUpdateDetails(
				rowId,
				updateGroupBuilder,
				oldValues,
				valuesAnalysis,
				(position, attribute) -> valuesAnalysis.getAttributeAnalyses().get( position ).isDirty(),
				session
		);

		return createOperationGroup( valuesAnalysis, updateGroupBuilder.buildMutationGroup() );
	}

	private void applyTableUpdateDetails(
			Object rowId,
			MutationGroupBuilder updateGroupBuilder,
			Object[] oldValues,
			UpdateValuesAnalysisImpl updateValuesAnalysis,
			DirtinessChecker dirtinessChecker,
			SharedSessionContractImplementor session) {
		final EntityVersionMapping versionMapping = entityPersister().getVersionMapping();
		final EntityRowIdMapping rowIdMapping = entityPersister().getRowIdMapping();
		final List<AttributeMapping> attributeMappings = entityPersister().getAttributeMappings();
		final boolean[] versionability = entityPersister().getPropertyVersionability();
		final OptimisticLockStyle optimisticLockStyle = entityPersister().optimisticLockStyle();

		updateGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final EntityTableMapping tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();

			final int[] attributeIndexes = tableMapping.getAttributeIndexes();
			for ( int i = 0; i < attributeIndexes.length; i++ ) {
				final int attributeIndex = attributeIndexes[i];

				final AttributeMapping attributeMapping = attributeMappings.get( attributeIndex );
				final AttributeAnalysis attributeAnalysis = updateValuesAnalysis.attributeAnalyses.get( attributeIndex );

				final TableUpdateBuilder<?> tableUpdateBuilder = (TableUpdateBuilder<?>) builder;

				if ( attributeAnalysis.includeInSet() ) {
					assert updateValuesAnalysis.tablesNeedingUpdate.contains( tableMapping );

					final Generator generator = attributeMapping.getGenerator();
					if ( isValueGenerationInSql( generator, dialect() ) ) {
						handleValueGeneration( attributeMapping, updateGroupBuilder, (InDatabaseGenerator) generator );
					}
					else if ( versionMapping != null
							&& versionMapping.getVersionAttribute() == attributeMapping ) {
						tableUpdateBuilder.addValueColumn( versionMapping.getVersionAttribute() );
					}
					else {
						final boolean includeInSet;
						if ( entityPersister().getEntityMetamodel().isDynamicUpdate() && dirtinessChecker != null ) {
							includeInSet = dirtinessChecker.isDirty( attributeIndex, attributeMapping );
						}
						else {
							includeInSet = true;
						}

						if ( includeInSet ) {
							attributeMapping.forEachSelectable( (selectionIndex, selectableMapping) -> {
								if ( selectableMapping.isFormula() ) {
									// no physical column
									return;
								}

								if ( !selectableMapping.isUpdateable() ) {
									// column is not updateable
									return;
								}

								tableUpdateBuilder.addValueColumn( selectableMapping );
							} );
						}
					}
				}

				if ( attributeAnalysis.includeInLocking() ) {
					final boolean includeRestriction;
					if ( optimisticLockStyle == OptimisticLockStyle.VERSION
							&& versionMapping != null
							&& attributeMapping == versionMapping.getVersionAttribute() ) {
						includeRestriction = true;
					}
					else if ( optimisticLockStyle == OptimisticLockStyle.ALL ) {
						includeRestriction = versionability[ attributeIndex ];
					}
					else if ( optimisticLockStyle == DIRTY ) {
						if ( dirtinessChecker == null ) {
							// this should indicate creation of the "static" update group.
							includeRestriction = false;
						}
						else {
							includeRestriction = versionability[ attributeIndex ]
									&& attributeAnalysis.includeInLocking()
									&& dirtinessChecker.isDirty( attributeIndex, attributeMapping );
						}
					}
					else {
						includeRestriction = false;
					}

					if ( includeRestriction ) {
						if ( oldValues == null ) {
							attributeMapping.forEachSelectable( (selectionIndex, selectableMapping) -> {
								tableUpdateBuilder.addOptimisticLockRestriction( selectableMapping );
							} );
						}
						else {
							attributeMapping.decompose(
									oldValues[ attributeIndex ],
									(jdbcValue, jdbcMapping) -> {
										if ( jdbcValue == null ) {
											tableUpdateBuilder.addNullOptimisticLockRestriction( jdbcMapping );
										}
										else {
											tableUpdateBuilder.addOptimisticLockRestriction( jdbcMapping );
										}
									},
									session
							);
						}
					}
				}
			}
		} );

		updateGroupBuilder.forEachTableMutationBuilder( (tableMutationBuilder) -> {
			final TableUpdateBuilder<?> tableUpdateBuilder = (TableUpdateBuilder<?>) tableMutationBuilder;
			final EntityTableMapping tableMapping = (EntityTableMapping) tableUpdateBuilder.getMutatingTable().getTableMapping();
			if ( rowIdMapping != null
					&& rowId != null
					&& tableMapping.isIdentifierTable() ) {
				tableUpdateBuilder.addKeyRestriction( rowIdMapping );
			}
			else {
				tableMapping.getKeyMapping().forEachKeyColumn( keyColumn -> tableUpdateBuilder.addKeyRestriction(
						keyColumn.getColumnName(),
						"?",
						keyColumn.getJdbcMapping()
				) );
			}
		} );
	}

	/**
	 * Contains the aggregated analysis of the update values to determine
	 * what SQL UPDATE statement(s) should be used to update the entity
	 * and to drive parameter binding
	 */
	private class UpdateValuesAnalysisImpl implements UpdateValuesAnalysis {
		private final Object[] values;
		private final int[] dirtyAttributeIndexes;
		private final InclusionChecker dirtinessChecker;

		private final Set<EntityTableMapping> tablesNeedingUpdate = new HashSet<>();
		private final Set<EntityTableMapping> tablesNeedingDynamicUpdate = new HashSet<>();
		private final Set<EntityTableMapping> tablesWithNonNullValues = new HashSet<>();
		private final Set<EntityTableMapping> tablesWithPreviousNonNullValues = new HashSet<>();

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
					}
				}
			} );
		}

		@Override
		public Object[] getValues() {
			return values;
		}

		@Override
		public Set<EntityTableMapping> getTablesNeedingUpdate() {
			return tablesNeedingUpdate;
		}

		@Override
		public Set<EntityTableMapping> getTablesWithNonNullValues() {
			return tablesWithNonNullValues;
		}

		@Override
		public Set<EntityTableMapping> getTablesWithPreviousNonNullValues() {
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
						|| ArrayHelper.contains( dirtyAttributeIndexes, attribute.getStateArrayPosition() ) ) {
					currentAttributeAnalysis.markDirty();
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
	}

	/**
	 * Local extension to AttributeAnalysis
	 */
	private interface AttributeAnalysisImplementor extends AttributeAnalysis {
		void markDirty();
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
		public boolean isDirty() {
			return false;
		}

		@Override
		public void markDirty() {
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

		private boolean dirty;

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
		public boolean isDirty() {
			return dirty;
		}

		@Internal
		@Override
		public void markDirty() {
			this.dirty = true;
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
				(index,attribute) -> isValueGenerationInSql( attribute.getGenerator(), dialect() )
						|| entityPersister().getPropertyUpdateability()[index],
				(index,attribute) -> {
					final OptimisticLockStyle optimisticLockStyle = entityPersister().optimisticLockStyle();
					if ( optimisticLockStyle.isAll() ) {
						return true;
					}

					return optimisticLockStyle == OptimisticLockStyle.VERSION
							&& entityPersister().getVersionMapping() != null
							&& attribute == entityPersister().getVersionMapping().getVersionAttribute();
				},
				(index,attribute) -> true,
				false,
				null
		);

		final MutationGroupBuilder updateGroupBuilder = new MutationGroupBuilder( MutationType.UPDATE, entityPersister() );

		entityPersister().forEachMutableTable( (tableMapping) -> {
			// NOTE : TableUpdateBuilderStandard handles custom sql-update mappings
			final TableUpdateBuilder<MutationOperation> tableUpdateBuilder = new TableUpdateBuilderStandard<>( entityPersister(), tableMapping, factory() );
			updateGroupBuilder.addTableDetailsBuilder( tableUpdateBuilder );
		} );

		// next, iterate each attribute and build the SET and WHERE clauses
		applyTableUpdateDetails(
				// row-id
				"", // pass anything here to generate the row id restriction if possible
				// the "collector"
				updateGroupBuilder,
				// oldValues
				null,
				valuesAnalysis,
				(position, attribute) -> valuesAnalysis.getAttributeAnalyses().get( position ).isDirty(),
				// session
				null
		);

		// build the mutation-group (SQL AST) and convert it into a jdbc-operations (SQL String, etc) group
		return createOperationGroup( valuesAnalysis, updateGroupBuilder.buildMutationGroup() );
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private MutationOperationGroup buildVersionUpdateGroup() {
		final EntityVersionMapping versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping == null ) {
			return null;
		}

		if ( entityPersister().getSuperMappingType() != null ) {
			return null;
		}

		final TableUpdateBuilderStandard updateBuilder = new TableUpdateBuilderStandard(
				entityPersister(),
				entityPersister().getIdentifierTableMapping(),
				factory()
		);

		updateBuilder.setSqlComment( "forced version increment for " + entityPersister().getRolePath() );

		updateBuilder.addValueColumn( versionMapping );

		entityPersister().getIdentifierMapping().forEachSelectable( (selectionIndex, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );

		updateBuilder.addOptimisticLockRestriction( versionMapping );

		final RestrictedTableMutation<MutationOperation> mutation = updateBuilder.buildMutation();

		//noinspection resource
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = factory()
				.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory();
		final SqlAstTranslator<JdbcMutationOperation> translator = sqlAstTranslatorFactory.buildModelMutationTranslator(
				(RestrictedTableMutation) mutation,
				factory()
		);

		final JdbcMutationOperation jdbcMutation = translator.translate( null, MutationQueryOptions.INSTANCE );
		return new MutationOperationGroupSingle( MutationType.UPDATE, entityPersister(), jdbcMutation );
	}

	@FunctionalInterface
	private interface InclusionChecker {
		boolean include(int position, SingularAttributeMapping attribute);
	}

	@FunctionalInterface
	private interface DirtinessChecker {
		boolean isDirty(int position, AttributeMapping attribute);
	}

	@Override
	public String toString() {
		return "UpdateCoordinatorStandard(" + entityPersister().getEntityName() + ")";
	}
}
