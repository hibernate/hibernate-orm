/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
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
	public String getCustomReadExpression() {
		return selectableMapping.getCustomReadExpression();
	}

	@Override
	public String getCustomWriteExpression() {
		return selectableMapping.getCustomWriteExpression();
	}

	@Override
	public String getColumnDefinition() {
		return selectableMapping.getColumnDefinition();
	}

	@Override
	public Long getLength() {
		return selectableMapping.getLength();
	}

	@Override
	public Integer getPrecision() {
		return selectableMapping.getPrecision();
	}

	@Override
	public Integer getScale() {
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
		final SqlSelection sqlSelection = resolveSqlSelection( navigablePath, tableGroup, true, null, creationState );

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				selectableMapping.getJdbcMapping(),
				navigablePath
		);
	}

	private SqlSelection resolveSqlSelection(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			boolean allowFkOptimization,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		final SqlExpressionResolver exprResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		final TableGroup targetTableGroup;
		// If the index is part of the element table group, we must use that explicitly here because the index is basic
		// and thus there is no index table group registered. The logic in the PluralTableGroup prevents from looking
		// into the element table group though because the element table group navigable path is not the parent of this navigable path
		if ( nature == Nature.INDEX &&  collectionDescriptor.getAttributeMapping().getIndexMetadata().getIndexPropertyName() != null ) {
			targetTableGroup = ( (PluralTableGroup) tableGroup ).getElementTableGroup();
		}
		else {
			targetTableGroup = tableGroup;
		}
		final TableReference tableReference = targetTableGroup.resolveTableReference(
				navigablePath,
				getContainingTableExpression(),
				allowFkOptimization
		);
		return exprResolver.resolveSqlSelection(
				exprResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableReference,
								selectableMapping.getSelectionExpression()
						),
						sqlAstProcessingState -> new ColumnReference(
								tableReference,
								selectableMapping,
								creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
						)
				),
				getJdbcMapping().getJdbcJavaType(),
				fetchParent,
				creationState.getSqlAstCreationState().getCreationContext().getSessionFactory().getTypeConfiguration()
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		resolveSqlSelection( navigablePath, tableGroup, true, null, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept( resolveSqlSelection( navigablePath, tableGroup, true, null, creationState ), getJdbcMapping() );
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
		ResultsLogger.RESULTS_LOGGER.debugf(
				"Generating Fetch for collection-part : `%s` -> `%s`",
				collectionDescriptor.getRole(),
				nature.getName()
		);

		NavigablePath parentNavigablePath = fetchablePath.getParent();
		if ( parentNavigablePath instanceof EntityIdentifierNavigablePath ) {
			parentNavigablePath = parentNavigablePath.getParent();
		}

		final TableGroup tableGroup = creationState.getSqlAstCreationState()
				.getFromClauseAccess()
				.findTableGroup( parentNavigablePath );
		final SqlSelection sqlSelection = resolveSqlSelection( fetchablePath, tableGroup, true, fetchParent, creationState );

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				FetchTiming.IMMEDIATE,
				creationState
		);
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return Collections.singletonList( getJdbcMapping() );
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
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		valueConsumer.consume( domainValue, this );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( selectableMapping.getJdbcMapping().getValueConverter() != null ) {
			return selectableMapping.getJdbcMapping().getValueConverter().toRelationalValue( value );
		}
		return value;
	}
}
