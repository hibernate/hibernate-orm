package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.IntFunction;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * Descriptor for foreign-keys
 */
public interface ForeignKeyDescriptor extends VirtualModelPart, ValuedModelPart {

	String PART_NAME = "{fk}";
	String TARGET_PART_NAME = "{fk-target}";

	@Nonnull
	@Override
	default String getPartName() {
		return PART_NAME;
	}

	@Nonnull
	String getKeyTable();

	@Nonnull
	String getTargetTable();


	@Nonnull
	ValuedModelPart getKeyPart();

	@Nonnull
	ValuedModelPart getTargetPart();

	boolean isKeyPart(@Nonnull ValuedModelPart modelPart);

	@Nonnull
	default ValuedModelPart getPart(@Nonnull Nature nature) {
		if ( nature == Nature.KEY ) {
			return getKeyPart();
		}
		else {
			return getTargetPart();
		}
	}

	@Nonnull
	Side getKeySide();

	@Nonnull
	Side getTargetSide();

	@Nonnull
	default Side getSide(@Nonnull Nature nature) {
		if ( nature == Nature.KEY ) {
			return getKeySide();
		}
		else {
			return getTargetSide();
		}
	}

	@Nonnull
	@Override
	default String getContainingTableExpression() {
		return getKeyTable();
	}

	/**
	 * Compare the 2 values
	 */
	int compare(@Nullable Object key1, @Nullable Object key2);

	/**
	 * Create a DomainResult for the referring-side of the fk
	 * The table group must be the one containing the target.
	 */
	@Nonnull
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	DomainResult<?> createKeyDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup targetTableGroup,
			@Nullable FetchParent fetchParent,
			@Nonnull DomainResultCreationState creationState);

	/**
	 * Create a DomainResult for the referring-side of the fk
	 * The table group must be the one containing the target.
	 * The {@link Nature} is the association side of the foreign key i.e. {@link Association#getSideNature()}.
	 */
	@Nonnull
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	DomainResult<?> createKeyDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup targetTableGroup,
			@Nonnull Nature fromSide,
			@Nullable FetchParent fetchParent,
			@Nonnull DomainResultCreationState creationState);

	/**
	 * Create a DomainResult for the target-side of the fk
	 * The table group must be the one containing the target
	 */
	@Nonnull
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	DomainResult<?> createTargetDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup targetTableGroup,
			@Nullable FetchParent fetchParent,
			@Nonnull DomainResultCreationState creationState);

	/**
	 * Create a DomainResult for the referring-side of the fk
	 * The table group must be the one containing the target.
	 */
	@Nonnull
	@Override
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	<T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup targetTableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState);

	@Nonnull
	Predicate generateJoinPredicate(
			@Nonnull TableGroup targetSideTableGroup,
			@Nonnull TableGroup keySideTableGroup,
			@Nonnull SqlAstCreationState creationState);

	@Nonnull
	Predicate generateJoinPredicate(
			@Nonnull TableReference targetSideReference,
			@Nonnull TableReference keySideReference,
			@Nonnull SqlAstCreationState creationState);

	boolean isSimpleJoinPredicate(@Nullable Predicate predicate);

	@Nonnull
	@Override
	SelectableMapping getSelectable(int columnIndex);

	/**
	 * Visits the FK "referring" columns
	 */
	@Override
	default int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
		return visitKeySelectables( offset, consumer );
	}

	@Nullable
	default Object getAssociationKeyFromSide(
			@Nullable Object targetObject,
			@Nonnull Nature nature,
			@Nullable SharedSessionContractImplementor session) {
		return getAssociationKeyFromSide( targetObject, getSide( nature ), session );
	}

	@Nullable
	Object getAssociationKeyFromSide(
			@Nullable Object targetObject,
			@Nonnull ForeignKeyDescriptor.Side side,
			@Nullable SharedSessionContractImplementor session);

	int visitKeySelectables(int offset, @Nonnull SelectableConsumer consumer);

	default int visitKeySelectables(@Nonnull SelectableConsumer consumer)  {
		return visitKeySelectables( 0, consumer );
	}

	int visitTargetSelectables(int offset, @Nonnull SelectableConsumer consumer);

	default int visitTargetSelectables(@Nonnull SelectableConsumer consumer) {
		return visitTargetSelectables( 0, consumer );
	}

	/**
	 * Return a copy of this foreign key descriptor with the selectable mappings as provided by the given accessor.
	 */
	@Nonnull
	@org.hibernate.Internal
	ForeignKeyDescriptor withKeySelectionMapping(
			@Nullable ManagedMappingType declaringType,
			@Nonnull TableGroupProducer declaringTableGroupProducer,
			@Nonnull IntFunction<SelectableMapping> selectableMappingAccess,
			@Nonnull MappingModelCreationProcess creationProcess);

	/**
	 * Return a copy of this foreign key descriptor with the target part as given by the argument.
	 */
	@Nonnull
	ForeignKeyDescriptor withTargetPart(@Nonnull ValuedModelPart targetPart);

	@Nonnull
	AssociationKey getAssociationKey();

	boolean hasConstraint();

	enum Nature {
		KEY,
		TARGET;

		@Nonnull
		public Nature inverse() {
			return this == KEY ? TARGET : KEY;
		}
	}

	interface Side {
		@Nonnull
		Nature getNature();
		@Nonnull
		ValuedModelPart getModelPart();
	}

	boolean isEmbedded();

}
