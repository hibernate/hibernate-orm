/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.SelectionConsumer;
import org.hibernate.metamodel.mapping.SelectionMapping;
import org.hibernate.metamodel.mapping.ConvertibleModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.EntityIdentifierNavigablePath;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Models a basic collection element/value or index/key
 *
 * @author Steve Ebersole
 */
public class BasicValuedCollectionPart
		implements CollectionPart, BasicValuedModelPart, ConvertibleModelPart, FetchOptions {
	private final NavigableRole navigableRole;
	private final CollectionPersister collectionDescriptor;
	private final Nature nature;
	private final BasicValueConverter valueConverter;

	private final SelectionMapping selectionMapping;

	public BasicValuedCollectionPart(
			CollectionPersister collectionDescriptor,
			Nature nature,
			BasicValueConverter valueConverter,
			SelectionMapping selectionMapping) {
		this.navigableRole = collectionDescriptor.getNavigableRole().append( nature.getName() );
		this.collectionDescriptor = collectionDescriptor;
		this.nature = nature;
		this.valueConverter = valueConverter;
		this.selectionMapping = selectionMapping;
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public MappingType getPartMappingType() {
		return selectionMapping.getJdbcMapping()::getJavaTypeDescriptor;
	}

	@Override
	public String getContainingTableExpression() {
		return selectionMapping.getContainingTableExpression();
	}

	@Override
	public String getSelectionExpression() {
		return selectionMapping.getSelectionExpression();
	}

	@Override
	public boolean isFormula() {
		return selectionMapping.isFormula();
	}

	@Override
	public String getCustomReadExpression() {
		return selectionMapping.getCustomReadExpression();
	}

	@Override
	public String getCustomWriteExpression() {
		return selectionMapping.getCustomWriteExpression();
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return valueConverter;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return selectionMapping.getJdbcMapping().getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = resolveSqlSelection( tableGroup, creationState );

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				getJavaTypeDescriptor(),
				valueConverter,
				navigablePath
		);
	}

	private SqlSelection resolveSqlSelection(TableGroup tableGroup, DomainResultCreationState creationState) {
		final SqlExpressionResolver exprResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();

		return exprResolver.resolveSqlSelection(
				exprResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableGroup.getPrimaryTableReference(),
								selectionMapping.getSelectionExpression()
						),
						sqlAstProcessingState -> new ColumnReference(
								tableGroup.getPrimaryTableReference().getIdentificationVariable(),
								selectionMapping,
								creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
						)
				),
				getJavaTypeDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {

	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return collectionDescriptor.getAttributeMapping().findContainingEntityMapping();
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return selectionMapping.getJdbcMapping();
	}

	@Override
	public MappingType getMappedType() {
		return this::getJavaTypeDescriptor;
	}

	@Override
	public String getFetchableName() {
		return nature == Nature.ELEMENT ? "{value}" : "{key}";
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
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		ResultsLogger.LOGGER.debugf(
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
		final SqlSelection sqlSelection = resolveSqlSelection( tableGroup, creationState );

		return new BasicFetch(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				false,
				valueConverter,
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
		action.accept( offset, selectionMapping.getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachSelection(int offset, SelectionConsumer consumer) {
		consumer.accept( offset, selectionMapping );
		return getJdbcTypeCount();
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
		if ( valueConverter != null ) {
			//noinspection unchecked
			return valueConverter.toRelationalValue( value );
		}
		return value;
	}
}
