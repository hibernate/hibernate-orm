/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Models a basic collection element/value or index/key
 *
 * @author Steve Ebersole
 */
public class BasicValuedCollectionPart
		implements CollectionPart, BasicValuedModelPart, FetchOptions {
	private final NavigableRole navigableRole;
	private final CollectionPersister collectionDescriptor;
	private final Nature nature;

	private final SelectableMapping selectableMapping;

	public BasicValuedCollectionPart(
			CollectionPersister collectionDescriptor,
			Nature nature,
			SelectableMapping selectableMapping) {
		this.navigableRole = collectionDescriptor.getNavigableRole().append( nature.getName() );
		this.collectionDescriptor = collectionDescriptor;
		this.nature = nature;
		this.selectableMapping = selectableMapping;
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public PluralAttributeMapping getCollectionAttribute() {
		return collectionDescriptor.getAttributeMapping();
	}

	@Override
	public MappingType getPartMappingType() {
		return selectableMapping.getJdbcMapping()::getJavaTypeDescriptor;
	}

	@Override
	public String getContainingTableExpression() {
		return selectableMapping.getContainingTableExpression();
	}

	@Override
	public String getSelectionExpression() {
		return selectableMapping.getSelectionExpression();
	}

	@Override
	public boolean isFormula() {
		return selectableMapping.isFormula();
	}

	@Override
	public boolean isNullable() {
		return selectableMapping.isNullable();
	}

	@Override
	public boolean isInsertable() {
		return selectableMapping.isInsertable();
	}

	@Override
	public boolean isPartitioned() {
		return selectableMapping.isPartitioned();
	}

	@Override
	public boolean isUpdateable() {
		return selectableMapping.isUpdateable();
	}

	@Override
	public @Nullable String getCustomReadExpression() {
		return selectableMapping.getCustomReadExpression();
	}

	@Override
	public @Nullable String getCustomWriteExpression() {
		return selectableMapping.getCustomWriteExpression();
	}

	@Override
	public @Nullable String getColumnDefinition() {
		return selectableMapping.getColumnDefinition();
	}

	@Override
	public @Nullable Long getLength() {
		return selectableMapping.getLength();
	}

	@Override
	public @Nullable Integer getArrayLength() {
		return selectableMapping.getArrayLength();
	}

	@Override
	public @Nullable Integer getPrecision() {
		return selectableMapping.getPrecision();
	}

	@Override
	public @Nullable Integer getTemporalPrecision() {
		return selectableMapping.getTemporalPrecision();
	}

	@Override
	public @Nullable Integer getScale() {
		return selectableMapping.getScale();
	}

	@Override
	public JavaType<?> getJavaType() {
		return selectableMapping.getJdbcMapping().getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String toString() {
		return "BasicValuedCollectionPart(" + navigableRole + ")@" + System.identityHashCode( this );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final var sqlSelection = resolveSqlSelection( navigablePath, tableGroup, null, creationState );
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				selectableMapping.getJdbcMapping(),
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	private SqlSelection resolveSqlSelection(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		final var exprResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		final TableGroup targetTableGroup;
		// If the index is part of the element table group, we must use that explicitly here because the index is basic
		// and thus there is no index table group registered. The logic in the PluralTableGroup prevents from looking
		// into the element table group though because the element table group navigable path is not the parent of this navigable path
		if ( nature == Nature.INDEX
				&& collectionDescriptor.getAttributeMapping().getIndexMetadata().getIndexPropertyName() != null ) {
			targetTableGroup = ( (PluralTableGroup) tableGroup ).getElementTableGroup();
		}
		else {
			targetTableGroup = tableGroup;
		}
		final var tableReference =
				targetTableGroup.resolveTableReference( navigablePath, getContainingTableExpression() );
		return exprResolver.resolveSqlSelection(
				exprResolver.resolveSqlExpression( tableReference, selectableMapping ),
				getJdbcMapping().getJdbcJavaType(),
				fetchParent,
				creationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration()
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		resolveSqlSelection( navigablePath, tableGroup, null, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept( resolveSqlSelection( navigablePath, tableGroup, null, creationState ), getJdbcMapping() );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return collectionDescriptor.getAttributeMapping().findContainingEntityMapping();
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return selectableMapping.getJdbcMapping();
	}

	@Override
	public MappingType getMappedType() {
		return this::getJavaType;
	}

	@Override
	public String getFetchableName() {
		return nature.getName();
	}

	@Override
	public int getFetchableKey() {
		return nature == Nature.INDEX || !collectionDescriptor.hasIndex() ? 0 : 1;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		if ( ResultsLogger.RESULTS_LOGGER.isTraceEnabled() ) {
			ResultsLogger.RESULTS_LOGGER.tracef(
					"Generating Fetch for collection-part: `%s` -> `%s`",
					collectionDescriptor.getRole(),
					nature.getName()
			);
		}

		NavigablePath parentNavigablePath = fetchablePath.getParent();
		if ( parentNavigablePath instanceof EntityIdentifierNavigablePath ) {
			parentNavigablePath = parentNavigablePath.getParent();
		}

		final var tableGroup =
				creationState.getSqlAstCreationState().getFromClauseAccess()
						.findTableGroup( parentNavigablePath );
		final var sqlSelection = resolveSqlSelection( fetchablePath, tableGroup, fetchParent, creationState );

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				FetchTiming.IMMEDIATE,
				creationState,
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( index );
		}
		return getJdbcMapping();
	}

	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return getJdbcMapping();
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, selectableMapping.getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, selectableMapping );
		return getJdbcTypeCount();
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		valueConsumer.consume( offset, x, y, disassemble( domainValue, session ), this );
		return getJdbcTypeCount();
	}

	@Override
	public <X, Y> int decompose(
			Object domainValue, int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		valueConsumer.consume( offset, x, y, disassemble( domainValue, session ), this );
		return getJdbcTypeCount();
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, x, y, value, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
