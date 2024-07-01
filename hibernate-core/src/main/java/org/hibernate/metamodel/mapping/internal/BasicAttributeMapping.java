/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class BasicAttributeMapping
		extends AbstractSingularAttributeMapping
		implements BasicValuedModelPart {
	private final NavigableRole navigableRole;

	private final String tableExpression;
	private final String mappedColumnExpression;
	private final Integer temporalPrecision;
	private final SelectablePath selectablePath;
	private final boolean isFormula;
	private final String customReadExpression;
	private final String customWriteExpression;
	private final String columnDefinition;
	private final Long length;
	private final Integer precision;
	private final Integer scale;

	private final JdbcMapping jdbcMapping;
	private final boolean isLob;
	private final boolean nullable;
	private final boolean insertable;
	private final boolean updateable;
	private final boolean partitioned;
	private final boolean isLazy;

	private final JavaType domainTypeDescriptor;

	public BasicAttributeMapping(
			String attributeName,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			AttributeMetadata attributeMetadata,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			String tableExpression,
			String mappedColumnExpression,
			SelectablePath selectablePath,
			boolean isFormula,
			String customReadExpression,
			String customWriteExpression,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			Integer temporalPrecision,
			boolean isLob,
			boolean nullable,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			JdbcMapping jdbcMapping,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super(
				attributeName,
				stateArrayPosition,
				fetchableIndex,
				attributeMetadata,
				mappedFetchTiming,
				mappedFetchStyle,
				declaringType,
				propertyAccess
		);
		this.navigableRole = navigableRole;
		this.tableExpression = tableExpression;
		this.mappedColumnExpression = mappedColumnExpression;
		this.temporalPrecision = temporalPrecision;
		if ( selectablePath == null ) {
			this.selectablePath = new SelectablePath( mappedColumnExpression );
		}
		else {
			this.selectablePath = selectablePath;
		}
		this.isFormula = isFormula;
		this.columnDefinition = columnDefinition;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
		this.isLob = isLob;
		this.nullable = nullable;
		this.insertable = insertable;
		this.updateable = updateable;
		this.partitioned = partitioned;
		this.jdbcMapping = jdbcMapping;
		this.domainTypeDescriptor = jdbcMapping.getJavaTypeDescriptor();

		this.customReadExpression = customReadExpression;

		if ( isFormula ) {
			this.customWriteExpression = null;
		}
		else {
			this.customWriteExpression = customWriteExpression;
		}
		this.isLazy = navigableRole.getParent().getParent() == null && declaringType.findContainingEntityMapping()
				.getEntityPersister()
				.getBytecodeEnhancementMetadata()
				.getLazyAttributesMetadata()
				.isLazyAttribute( attributeName );
	}

	public static BasicAttributeMapping withSelectableMapping(
			ManagedMappingType declaringType,
			BasicValuedModelPart original,
			PropertyAccess propertyAccess,
			boolean insertable,
			boolean updateable,
			SelectableMapping selectableMapping) {
		String attributeName = null;
		int stateArrayPosition = 0;
		AttributeMetadata attributeMetadata;
		if ( original instanceof SingleAttributeIdentifierMapping ) {
			final SingleAttributeIdentifierMapping mapping = (SingleAttributeIdentifierMapping) original;
			attributeName = mapping.getAttributeName();
			attributeMetadata = new SimpleAttributeMetadata(
					propertyAccess,
					mapping.getExpressibleJavaType().getMutabilityPlan(),
					selectableMapping.isNullable(),
					insertable,
					updateable,
					false,
					true
			);
		}
		else if ( original instanceof SingularAttributeMapping ) {
			final SingularAttributeMapping mapping = (SingularAttributeMapping) original;
			attributeName = mapping.getAttributeName();
			stateArrayPosition = mapping.getStateArrayPosition();
			attributeMetadata = mapping.getAttributeMetadata();
		}
		else {
			attributeMetadata = null;
		}
		return new BasicAttributeMapping(
				attributeName,
				original.getNavigableRole(),
				stateArrayPosition,
				original.getFetchableKey(),
				attributeMetadata,
				FetchTiming.IMMEDIATE,
				FetchStyle.JOIN,
				selectableMapping.getContainingTableExpression(),
				selectableMapping.getSelectionExpression(),
				selectableMapping.getSelectablePath(),
				selectableMapping.isFormula(),
				selectableMapping.getCustomReadExpression(),
				selectableMapping.getCustomWriteExpression(),
				selectableMapping.getColumnDefinition(),
				selectableMapping.getLength(),
				selectableMapping.getPrecision(),
				selectableMapping.getScale(),
				selectableMapping.getTemporalPrecision(),
				selectableMapping.isLob(),
				selectableMapping.isNullable(),
				insertable,
				updateable,
				selectableMapping.isPartitioned(),
				original.getJdbcMapping(),
				declaringType,
				propertyAccess
		);
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public MappingType getMappedType() {
		return getJdbcMapping();
	}

	@Override
	public JavaType<?> getJavaType() {
		return domainTypeDescriptor;
	}

	@Override
	public String getSelectionExpression() {
		return mappedColumnExpression;
	}

	@Override
	public String getSelectableName() {
		return selectablePath.getSelectableName();
	}

	@Override
	public SelectablePath getSelectablePath() {
		return selectablePath;
	}

	@Override
	public boolean isLob() {
		return isLob;
	}

	@Override
	public boolean isFormula() {
		return isFormula;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	public boolean isLazy() {
		return isLazy;
	}

	@Override
	public boolean isInsertable() {
		return insertable;
	}

	@Override
	public boolean isUpdateable() {
		return updateable;
	}

	@Override
	public boolean isPartitioned() {
		return partitioned;
	}

	@Override
	public String getCustomReadExpression() {
		return customReadExpression;
	}

	@Override
	public String getCustomWriteExpression() {
		return customWriteExpression;
	}

	@Override
	public String getWriteExpression() {
		return customWriteExpression;
	}

	@Override
	public String getColumnDefinition() {
		return columnDefinition;
	}

	@Override
	public Long getLength() {
		return length;
	}

	@Override
	public Integer getPrecision() {
		return precision;
	}

	@Override
	public Integer getScale() {
		return scale;
	}

	@Override
	public Integer getTemporalPrecision() {
		return temporalPrecision;
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String toString() {
		return "BasicAttributeMapping(" + navigableRole + ")@" + System.identityHashCode( this );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = resolveSqlSelection( navigablePath, tableGroup, null, creationState );

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				jdbcMapping,
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
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference(
				navigablePath,
				this,
				getContainingTableExpression()
		);

		return expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression(
						tableReference,
						this
				),
				jdbcMapping.getJdbcJavaType(),
				fetchParent,
				creationState.getSqlAstCreationState().getCreationContext().getSessionFactory().getTypeConfiguration()
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
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
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final int valuesArrayPosition;
		boolean coerceResultType = false;
		final SqlSelection sqlSelection;
		if ( fetchTiming == FetchTiming.DELAYED && isLazy ) {
			// Lazy property. A valuesArrayPosition of -1 will lead to
			// returning a domain result assembler that returns LazyPropertyInitializer.UNFETCHED_PROPERTY
			valuesArrayPosition = -1;
			sqlSelection = null;
		}
		else {
			final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
			final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
					fetchParent.getNavigablePath()
			);

			assert tableGroup != null;

			sqlSelection = resolveSqlSelection(
					fetchablePath,
					tableGroup,
					fetchParent,
					creationState
			);
			valuesArrayPosition = sqlSelection.getValuesArrayPosition();
			if ( sqlSelection.getExpressionType() != null) {
				// if the expression type is different that the expected type coerce the value
				coerceResultType = sqlSelection.getExpressionType().getSingleJdbcMapping().getJdbcJavaType() != getJdbcMapping().getJdbcJavaType();
			}
		}

		return new BasicFetch<>(
				valuesArrayPosition,
				fetchParent,
				fetchablePath,
				this,
				getJdbcMapping().getValueConverter(),
				fetchTiming,
				true,
				creationState,
				coerceResultType,
				sqlSelection != null && !sqlSelection.isVirtual()
		);
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

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, this );
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
}
