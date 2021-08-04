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
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ConvertibleModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.tuple.ValueGeneration;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class BasicAttributeMapping
		extends AbstractSingularAttributeMapping
		implements SingularAttributeMapping, BasicValuedModelPart, ConvertibleModelPart {
	private final NavigableRole navigableRole;

	private final String tableExpression;
	private final String mappedColumnExpression;
	private final boolean isFormula;
	private final String customReadExpression;
	private final String customWriteExpression;

	private final JdbcMapping jdbcMapping;
	private final BasicValueConverter<Object, ?> valueConverter;

	private final JavaTypeDescriptor domainTypeDescriptor;

	@SuppressWarnings("WeakerAccess")
	public BasicAttributeMapping(
			String attributeName,
			NavigableRole navigableRole,
			int stateArrayPosition,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			String tableExpression,
			String mappedColumnExpression,
			boolean isFormula,
			String customReadExpression,
			String customWriteExpression,
			BasicValueConverter valueConverter,
			JdbcMapping jdbcMapping,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess,
			ValueGeneration valueGeneration) {
		super( attributeName, stateArrayPosition, attributeMetadataAccess, mappedFetchTiming, mappedFetchStyle, declaringType, propertyAccess, valueGeneration );
		this.navigableRole = navigableRole;
		this.tableExpression = tableExpression;
		this.mappedColumnExpression = mappedColumnExpression;
		this.isFormula = isFormula;
		this.valueConverter = valueConverter;
		this.jdbcMapping = jdbcMapping;

		if ( valueConverter == null ) {
			domainTypeDescriptor = jdbcMapping.getJavaTypeDescriptor();
		}
		else {
			domainTypeDescriptor = valueConverter.getDomainJavaDescriptor();
		}

		this.customReadExpression = customReadExpression;

		if ( isFormula ) {
			this.customWriteExpression = null;
		}
		else {
			this.customWriteExpression = customWriteExpression;
		}
	}

	public static BasicAttributeMapping withSelectableMapping(
			BasicValuedModelPart original,
			PropertyAccess propertyAccess,
			ValueGeneration valueGeneration,
			SelectableMapping selectableMapping) {
		String attributeName = null;
		int stateArrayPosition = 0;
		StateArrayContributorMetadataAccess attributeMetadataAccess = null;
		BasicValueConverter<?, ?> valueConverter = null;
		ManagedMappingType declaringType = null;
		if ( original instanceof SingleAttributeIdentifierMapping ) {
			final SingleAttributeIdentifierMapping mapping = (SingleAttributeIdentifierMapping) original;
			attributeName = mapping.getAttributeName();
			attributeMetadataAccess = null;
			declaringType = mapping.findContainingEntityMapping();
		}
		else if ( original instanceof SingularAttributeMapping ) {
			final SingularAttributeMapping mapping = (SingularAttributeMapping) original;
			attributeName = mapping.getAttributeName();
			stateArrayPosition = mapping.getStateArrayPosition();
			attributeMetadataAccess = mapping.getAttributeMetadataAccess();
			declaringType = mapping.getDeclaringType();
		}
		if ( original instanceof ConvertibleModelPart ) {
			valueConverter = ( (ConvertibleModelPart) original ).getValueConverter();
		}
		return new BasicAttributeMapping(
				attributeName,
				original.getNavigableRole(),
				stateArrayPosition,
				attributeMetadataAccess,
				FetchTiming.IMMEDIATE,
				FetchStyle.JOIN,
				selectableMapping.getContainingTableExpression(),
				selectableMapping.getSelectionExpression(),
				selectableMapping.isFormula(),
				selectableMapping.getCustomReadExpression(),
				selectableMapping.getCustomWriteExpression(),
				valueConverter,
				original.getJdbcMapping(),
				declaringType,
				propertyAccess,
				valueGeneration
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
	public JavaTypeDescriptor<?> getJavaTypeDescriptor() {
		return domainTypeDescriptor;
	}

	@Override
	public String getSelectionExpression() {
		return mappedColumnExpression;
	}

	@Override
	public boolean isFormula() {
		return isFormula;
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
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return valueConverter;
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
		final SqlSelection sqlSelection = resolveSqlSelection( tableGroup, creationState );

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				getMappedType().getMappedJavaTypeDescriptor(),
				valueConverter,
				navigablePath
		);
	}

	private SqlSelection resolveSqlSelection(TableGroup tableGroup, DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference(
				tableGroup.getNavigablePath()
						.append( getNavigableRole().getNavigableName() ),
				getContainingTableExpression()
		);
		final String tableAlias = tableReference.getIdentificationVariable();
		return expressionResolver.resolveSqlSelection(
				expressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableReference,
								mappedColumnExpression
						),
						sqlAstProcessingState -> new ColumnReference(
								tableAlias,
								this,
								creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
						)
				),
				valueConverter == null ? getMappedType().getMappedJavaTypeDescriptor() : valueConverter.getRelationalJavaDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		resolveSqlSelection( tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept( resolveSqlSelection( tableGroup, creationState ), getJdbcMapping() );
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
		// Lazy property. A valuesArrayPosition of -1 will lead to
		// returning a domain result assembler that returns LazyPropertyInitializer.UNFETCHED_PROPERTY
		final EntityMappingType containingEntityMapping = findContainingEntityMapping();
		if ( fetchTiming == FetchTiming.DELAYED
				&& fetchParent.getReferencedModePart() == containingEntityMapping
				&& containingEntityMapping.getEntityPersister().getPropertyLaziness()[getStateArrayPosition()] ) {
			valuesArrayPosition = -1;
		}
		else {
			final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
			final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
					fetchParent.getNavigablePath()
			);

			assert tableGroup != null;

			final SqlSelection sqlSelection = resolveSqlSelection( tableGroup, creationState );
			valuesArrayPosition = sqlSelection.getValuesArrayPosition();
		}

		return new BasicFetch<>(
				valuesArrayPosition,
				fetchParent,
				fetchablePath,
				this,
				getAttributeMetadataAccess().resolveAttributeMetadata( null ).isNullable(),
				valueConverter,
				fetchTiming,
				creationState
		);
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( valueConverter != null ) {
			return valueConverter.toRelationalValue( value );
		}
		return value;
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
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		valueConsumer.consume( disassemble( domainValue, session ), this );
	}
}
