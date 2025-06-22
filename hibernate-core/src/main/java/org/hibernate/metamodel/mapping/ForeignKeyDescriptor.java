/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.function.IntFunction;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * Descriptor for foreign-keys
 */
public interface ForeignKeyDescriptor extends VirtualModelPart, ValuedModelPart {

	String PART_NAME = "{fk}";
	String TARGET_PART_NAME = "{fk-target}";

	@Override
	default String getPartName() {
		return PART_NAME;
	}

	String getKeyTable();

	String getTargetTable();


	ValuedModelPart getKeyPart();

	ValuedModelPart getTargetPart();

	boolean isKeyPart(ValuedModelPart modelPart);

	default ValuedModelPart getPart(Nature nature) {
		if ( nature == Nature.KEY ) {
			return getKeyPart();
		}
		else {
			return getTargetPart();
		}
	}

	Side getKeySide();

	Side getTargetSide();

	default Side getSide(Nature nature) {
		if ( nature == Nature.KEY ) {
			return getKeySide();
		}
		else {
			return getTargetSide();
		}
	}

	@Override
	default String getContainingTableExpression() {
		return getKeyTable();
	}

	/**
	 * Compare the 2 values
	 */
	int compare(Object key1, Object key2);

	/**
	 * Create a DomainResult for the referring-side of the fk
	 * The table group must be the one containing the target.
	 */
	DomainResult<?> createKeyDomainResult(
			NavigablePath navigablePath,
			TableGroup targetTableGroup,
			FetchParent fetchParent,
			DomainResultCreationState creationState);

	/**
	 * Create a DomainResult for the referring-side of the fk
	 * The table group must be the one containing the target.
	 * The {@link Nature} is the association side of the foreign key i.e. {@link Association#getSideNature()}.
	 */
	DomainResult<?> createKeyDomainResult(
			NavigablePath navigablePath,
			TableGroup targetTableGroup,
			Nature fromSide,
			FetchParent fetchParent,
			DomainResultCreationState creationState);

	/**
	 * Create a DomainResult for the target-side of the fk
	 * The table group must be the one containing the target
	 */
	DomainResult<?> createTargetDomainResult(
			NavigablePath navigablePath,
			TableGroup targetTableGroup,
			FetchParent fetchParent,
			DomainResultCreationState creationState);

	/**
	 * Create a DomainResult for the referring-side of the fk
	 * The table group must be the one containing the target.
	 */
	@Override
	<T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup targetTableGroup,
			String resultVariable,
			DomainResultCreationState creationState);

	Predicate generateJoinPredicate(
			TableGroup targetSideTableGroup,
			TableGroup keySideTableGroup,
			SqlAstCreationState creationState);

	Predicate generateJoinPredicate(
			TableReference targetSideReference,
			TableReference keySideReference,
			SqlAstCreationState creationState);

	boolean isSimpleJoinPredicate(Predicate predicate);

	@Override
	SelectableMapping getSelectable(int columnIndex);

	/**
	 * Visits the FK "referring" columns
	 */
	@Override
	default int forEachSelectable(int offset, SelectableConsumer consumer) {
		return visitKeySelectables( offset, consumer );
	}

	default Object getAssociationKeyFromSide(
			Object targetObject,
			Nature nature,
			SharedSessionContractImplementor session) {
		return getAssociationKeyFromSide( targetObject, getSide( nature ), session );
	}

	Object getAssociationKeyFromSide(
			Object targetObject,
			ForeignKeyDescriptor.Side side,
			SharedSessionContractImplementor session);

	int visitKeySelectables(int offset, SelectableConsumer consumer);

	default int visitKeySelectables(SelectableConsumer consumer)  {
		return visitKeySelectables( 0, consumer );
	}

	int visitTargetSelectables(int offset, SelectableConsumer consumer);

	default int visitTargetSelectables(SelectableConsumer consumer) {
		return visitTargetSelectables( 0, consumer );
	}

	/**
	 * Return a copy of this foreign key descriptor with the selectable mappings as provided by the given accessor.
	 */
	ForeignKeyDescriptor withKeySelectionMapping(
			ManagedMappingType declaringType,
			TableGroupProducer declaringTableGroupProducer,
			IntFunction<SelectableMapping> selectableMappingAccess,
			MappingModelCreationProcess creationProcess);

	/**
	 * Return a copy of this foreign key descriptor with the target part as given by the argument.
	 */
	ForeignKeyDescriptor withTargetPart(ValuedModelPart targetPart);

	AssociationKey getAssociationKey();

	boolean hasConstraint();

	enum Nature {
		KEY,
		TARGET;

		public Nature inverse() {
			return this == KEY ? TARGET : KEY;
		}
	}

	interface Side {
		Nature getNature();
		ValuedModelPart getModelPart();
	}

	boolean isEmbedded();

}
