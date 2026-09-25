package org.hibernate.persister.collection.mutation;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.UnaryOperator;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.audit.AuditStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AuditMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.OneToManyCollectionPart;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilderStandard;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;

import static org.hibernate.audit.AuditStrategy.VALIDITY;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Support for building audit log mutations for collections.
 */
public final class AuditCollectionHelper {
	private final CollectionMutationTarget mutationTarget;
	private final SessionFactoryImplementor sessionFactory;
	private final CollectionTableMapping auditTableMapping;
	private final SelectableMapping changesetIdMapping;
	private final SelectableMapping modificationTypeMapping;
	@Nullable
	private final SelectableMapping transactionEndMapping;
	private final AuditStrategy auditStrategy;
	private final boolean useServerTransactionTimestamps;
	@Nullable
	private final String currentTimestampFunctionName;
	@Nullable
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	@Nullable
	private MutationOperationGroup auditInsertOperationGroup;
	@Nullable
	private MutationOperationGroup transactionEndUpdateGroup;
	@Nullable
	private AuditCollectionRowMutationHelper rowMutationHelper;

	AuditCollectionHelper(
			@Nonnull CollectionMutationTarget mutationTarget,
			@Nonnull SessionFactoryImplementor sessionFactory,
			@Nullable boolean[] indexColumnIsSettable,
			@Nonnull boolean[] elementColumnIsSettable,
			@Nonnull UnaryOperator<Object> indexIncrementer,
			@Nonnull AuditMapping auditMapping) {
		this.mutationTarget = mutationTarget;
		this.sessionFactory = sessionFactory;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		final String collectionTableName = mutationTarget.getCollectionTableMapping().getTableName();
		this.auditTableMapping = new CollectionTableMapping(
				mutationTarget.getCollectionTableMapping(),
				auditMapping.resolveTableName( collectionTableName )
		);
		this.changesetIdMapping = auditMapping.getChangesetIdMapping( collectionTableName );
		this.modificationTypeMapping = castNonNull( auditMapping.getModificationTypeMapping( collectionTableName ) );
		this.transactionEndMapping = auditMapping.getInvalidatingChangesetIdMapping( collectionTableName );
		this.auditStrategy = sessionFactory.getSessionFactoryOptions().getAuditStrategy();

		final var dialect = sessionFactory.getJdbcServices().getDialect();
		this.useServerTransactionTimestamps =
				sessionFactory.getChangesetCoordinator()
						.useServerTimestamp( dialect );
		this.currentTimestampFunctionName = useServerTransactionTimestamps
				? dialect.getCurrentTemporalSupport().currentTimestamp()
				: null;
	}

	@Nonnull
	CollectionTableMapping getAuditTableMapping() {
		return auditTableMapping;
	}

	boolean useServerTransactionTimestamps() {
		return useServerTransactionTimestamps;
	}

	@Nullable
	MutationOperationGroup getAuditInsertOperationGroup() {
		if ( auditInsertOperationGroup == null ) {
			auditInsertOperationGroup = buildAuditInsertOperationGroup();
		}
		return auditInsertOperationGroup;
	}

	@Nonnull
	AuditCollectionRowMutationHelper getRowMutationHelper() {
		if ( rowMutationHelper == null ) {
			rowMutationHelper = new AuditCollectionRowMutationHelper(
					mutationTarget,
					auditTableMapping.getTableName(),
					changesetIdMapping,
					modificationTypeMapping,
					indexColumnIsSettable,
					elementColumnIsSettable,
					indexIncrementer,
					useServerTransactionTimestamps
			);
		}
		return rowMutationHelper;
	}

	@Nullable
	MutationOperationGroup getTransactionEndUpdateGroup() {
		if ( transactionEndUpdateGroup == null && auditStrategy == VALIDITY && transactionEndMapping != null ) {
			transactionEndUpdateGroup = buildTransactionEndUpdateGroup();
		}
		return transactionEndUpdateGroup;
	}

	@Nullable
	private MutationOperationGroup buildAuditInsertOperationGroup() {
		final var insertBuilder =
				new TableInsertBuilderStandard( mutationTarget, auditTableMapping, sessionFactory );
		applyAuditInsertDetails( insertBuilder );
		final var tableInsert = insertBuilder.buildMutation();
		final var operation = tableInsert.createMutationOperation( null, sessionFactory );
		return operation == null ? null : singleOperation( MutationType.INSERT, mutationTarget, operation );
	}

	private void applyAuditInsertDetails(@Nonnull TableInsertBuilderStandard insertBuilder) {
		final var attributeMapping = mutationTarget.getTargetPart();
		attributeMapping.getKeyDescriptor().getKeyPart().forEachSelectable( insertBuilder );

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.forEachSelectable( insertBuilder );
		}
		else {
			final var indexDescriptor = attributeMapping.getIndexDescriptor();
			if ( indexDescriptor != null ) {
				indexDescriptor.forEachInsertable( insertBuilder );
			}
		}

		final var elementDescriptor = attributeMapping.getElementDescriptor();
		if ( elementDescriptor instanceof OneToManyCollectionPart oneToMany ) {
			// For @OneToMany @JoinColumn, the middle audit table stores the child entity's ID,
			// not the FK columns (which are the element's selectables for OneToManyCollectionPart)
			oneToMany.getAssociatedEntityMappingType().getIdentifierMapping().forEachInsertable( insertBuilder );
		}
		else {
			elementDescriptor.forEachInsertable( insertBuilder );
		}

		if ( useServerTransactionTimestamps ) {
			insertBuilder.addValueColumn( currentTimestampFunctionName, changesetIdMapping );
		}
		else {
			insertBuilder.addValueColumn( "?", changesetIdMapping );
		}
		insertBuilder.addValueColumn( "?", modificationTypeMapping );
	}

	@Nullable
	private MutationOperationGroup buildTransactionEndUpdateGroup() {
		final var updateBuilder =
				new TableUpdateBuilderStandard<>( mutationTarget, auditTableMapping, sessionFactory );
		final var attributeMapping = mutationTarget.getTargetPart();

		// SET REVEND = ?
		if ( useServerTransactionTimestamps ) {
			updateBuilder.addValueColumn( currentTimestampFunctionName, transactionEndMapping );
		}
		else {
			updateBuilder.addValueColumn( "?", transactionEndMapping );
		}

		// WHERE: same identity columns as the INSERT (key + index/identifier + element)
		attributeMapping.getKeyDescriptor()
				.getKeyPart()
				.forEachSelectable( (index, selectable) -> updateBuilder.addKeyRestrictionBinding( selectable ) );

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.forEachSelectable( (index, selectable) -> updateBuilder.addKeyRestrictionBinding( selectable ) );
		}
		else {
			final var indexDescriptor = attributeMapping.getIndexDescriptor();
			if ( indexDescriptor != null ) {
				indexDescriptor.forEachSelectable( (index, selectable) -> updateBuilder.addKeyRestrictionBinding( selectable ) );
			}
		}

		final var elementDescriptor = attributeMapping.getElementDescriptor();
		if ( elementDescriptor instanceof OneToManyCollectionPart oneToMany ) {
			oneToMany.getAssociatedEntityMappingType()
					.getIdentifierMapping()
					.forEachSelectable( (index, selectable) -> updateBuilder.addKeyRestrictionBinding( selectable ) );
		}
		else {
			elementDescriptor.forEachSelectable( (index, selectable) -> updateBuilder.addKeyRestrictionBinding( selectable ) );
		}

		// WHERE REVEND IS NULL
		final var revEndColumnRef = new ColumnReference(
				updateBuilder.getMutatingTable(), transactionEndMapping );
		updateBuilder.addNonKeyRestriction( new ColumnValueBinding(
				revEndColumnRef,
				new ColumnWriteFragment( null, List.of(), transactionEndMapping )
		) );

		final var tableUpdate = updateBuilder.buildMutation();
		final var operation = tableUpdate.createMutationOperation( null, sessionFactory );
		return operation == null ? null : singleOperation( MutationType.UPDATE, mutationTarget, operation );
	}
}
