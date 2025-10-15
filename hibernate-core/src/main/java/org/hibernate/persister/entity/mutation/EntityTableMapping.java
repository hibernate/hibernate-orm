/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/**
 * Descriptor for the mapping of a table relative to an entity
 *
 * @author Steve Ebersole
 */
public class EntityTableMapping implements TableMapping {
	private enum Flag {
		OPTIONAL,
		INVERSE,
		ID_TABLE,
		CASCADE_DELETE
	}

	private final String tableName;
	private final int relativePosition;
	private final KeyMapping keyMapping;

	private final BitSet flags = new BitSet();

	private final int[] attributeIndexes;

	private final MutationDetails insertDetails;
	private final MutationDetails updateDetails;
	private final MutationDetails deleteDetails;

	public EntityTableMapping(
			String tableName,
			int relativePosition,
			KeyMapping keyMapping,
			boolean isOptional,
			boolean isInverse,
			boolean isIdentifierTable,
			int[] attributeIndexes,
			Expectation insertExpectation,
			String insertCustomSql,
			boolean insertCallable,
			Expectation updateExpectation,
			String updateCustomSql,
			boolean updateCallable,
			boolean cascadeDeleteEnabled,
			Expectation deleteExpectation,
			String deleteCustomSql,
			boolean deleteCallable,
			boolean dynamicUpdate,
			boolean dynamicInsert) {
		this.tableName = tableName;
		this.relativePosition = relativePosition;
		this.keyMapping = keyMapping;
		this.attributeIndexes = attributeIndexes;
		this.insertDetails = new MutationDetails(
				MutationType.INSERT,
				insertExpectation,
				insertCustomSql,
				insertCallable,
				dynamicInsert
		);
		this.updateDetails = new MutationDetails(
				MutationType.UPDATE,
				updateExpectation,
				updateCustomSql,
				updateCallable,
				dynamicUpdate
		);
		this.deleteDetails = new MutationDetails(
				MutationType.DELETE,
				deleteExpectation,
				deleteCustomSql,
				deleteCallable
		);

		if ( isOptional ) {
			flags.set( Flag.OPTIONAL.ordinal() );
		}

		if ( isInverse ) {
			flags.set( Flag.INVERSE.ordinal() );
		}

		if ( isIdentifierTable ) {
			flags.set( Flag.ID_TABLE.ordinal() );
		}

		if ( cascadeDeleteEnabled ) {
			flags.set( Flag.CASCADE_DELETE.ordinal() );
		}
	}

	@Override public String getTableName() {
		return tableName;
	}

	@Override
	public KeyDetails getKeyDetails() {
		return keyMapping;
	}

	@Override public int getRelativePosition() {
		return relativePosition;
	}

	@Override public boolean isOptional() {
		return flags.get( Flag.OPTIONAL.ordinal() );
	}

	@Override public boolean isInverse() {
		return flags.get( Flag.INVERSE.ordinal() );
	}

	@Override public boolean isIdentifierTable() {
		return flags.get( Flag.ID_TABLE.ordinal() );
	}

	public KeyMapping getKeyMapping() {
		return keyMapping;
	}

	public boolean hasColumns() {
		return attributeIndexes.length > 0;
	}

	public boolean containsAttributeColumns(int attributeIndex) {
		return ArrayHelper.contains( attributeIndexes, attributeIndex );
	}

	public int[] getAttributeIndexes() {
		return attributeIndexes;
	}

	@Override public MutationDetails getInsertDetails() {
		return insertDetails;
	}

	public Expectation getInsertExpectation() {
		return getInsertDetails().getExpectation();
	}

	public String getInsertCustomSql() {
		return getInsertDetails().getCustomSql();
	}

	public boolean isInsertCallable() {
		return getInsertDetails().isCallable();
	}

	@Override public MutationDetails getUpdateDetails() {
		return updateDetails;
	}

	public Expectation getUpdateExpectation() {
		return getUpdateDetails().getExpectation();
	}

	public String getUpdateCustomSql() {
		return getUpdateDetails().getCustomSql();
	}

	public boolean isUpdateCallable() {
		return getUpdateDetails().isCallable();
	}

	@Override public boolean isCascadeDeleteEnabled() {
		return flags.get( Flag.CASCADE_DELETE.ordinal() );
	}

	@Override public MutationDetails getDeleteDetails() {
		return deleteDetails;
	}

	public Expectation getDeleteExpectation() {
		return getDeleteDetails().getExpectation();
	}

	public String getDeleteCustomSql() {
		return getDeleteDetails().getCustomSql();
	}

	public boolean isDeleteCallable() {
		return getDeleteDetails().isCallable();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		final EntityTableMapping that = (EntityTableMapping) o;
		return tableName.equals( that.tableName );
	}

	@Override
	public int hashCode() {
		return Objects.hash( tableName );
	}

	@Override
	public String toString() {
		return "TableMapping(" + tableName + ")";
	}

	public interface KeyMapping extends KeyDetails, SelectableMappings {
	}

	public static KeyMapping createKeyMapping(List<KeyColumn> keyColumns, ModelPart identifierPart) {
		if ( identifierPart instanceof EmbeddableValuedModelPart embeddedModelPart ) {
			return new CompositeKeyMapping( keyColumns, embeddedModelPart );
		}
		else {
			assert keyColumns.size() == 1;
			return new SimpleKeyMapping( keyColumns, (BasicValuedModelPart) identifierPart );
		}
	}

	public static abstract class AbstractKeyMapping implements KeyMapping {
		protected final List<KeyColumn> keyColumns;
		protected final ModelPart identifierPart;

		public AbstractKeyMapping(List<KeyColumn> keyColumns, ModelPart identifierPart) {
			this.keyColumns = keyColumns;
			this.identifierPart = identifierPart;
		}

		@Override
		public List<? extends KeyColumn> getKeyColumns() {
			return keyColumns;
		}

		@Override
		public int getColumnCount() {
			return getKeyColumns().size();
		}

		@Override
		public KeyColumn getKeyColumn(int position) {
			return getKeyColumns().get( position );
		}

		@Override
		public void forEachKeyColumn(KeyColumnConsumer consumer) {
			for ( int i = 0; i < getKeyColumns().size(); i++ ) {
				consumer.consume( i, getKeyColumns().get( i ) );
			}
		}

		@Override
		public int getJdbcTypeCount() {
			return getKeyColumns().size();
		}

		@Override
		public SelectableMapping getSelectable(int columnIndex) {
			return getKeyColumns().get( columnIndex );
		}

		@Override
		public int forEachSelectable(int offset, SelectableConsumer consumer) {
			for ( int i = 0; i < getKeyColumns().size(); i++ ) {
				consumer.accept( i, getKeyColumns().get( i ) );
			}

			return getJdbcTypeCount();
		}

		public void breakDownKeyJdbcValues(
				Object domainValue,
				KeyValueConsumer valueConsumer,
				SharedSessionContractImplementor session) {
			identifierPart.forEachJdbcValue(
					domainValue,
					getKeyColumns(),
					valueConsumer,
					(selectionIndex, keys, consumer, jdbcValue, jdbcMapping) -> consumer.consume(
							jdbcValue,
							keys.get( selectionIndex )
					),
					session
			);
		}

		protected SqlSelection resolveSqlSelection(
				TableReference tableReference,
				KeyColumn keyColumn,
				SqlAstCreationState creationState) {
			final SqlExpressionResolver expressionResolver = creationState.getSqlExpressionResolver();
			return expressionResolver.resolveSqlSelection(
					expressionResolver.resolveSqlExpression( tableReference, keyColumn ),
					keyColumn.getJdbcMapping().getJdbcJavaType(),
					null,
					creationState.getCreationContext().getTypeConfiguration()
			);
		}

		@Override
		public int forEachSelectable(SelectableConsumer consumer) {
			forEachKeyColumn( consumer::accept );
			return getJdbcTypeCount();
		}
	}

	public static class SimpleKeyMapping extends AbstractKeyMapping {
		private final KeyColumn keyColumn;

		public SimpleKeyMapping(List<KeyColumn> keyColumns, BasicValuedModelPart identifierPart) {
			super( keyColumns, identifierPart );
			this.keyColumn = keyColumns.get( 0 );
		}

		@Override
		public <K> DomainResult<K> createDomainResult(
				NavigablePath navigablePath,
				TableReference tableReference,
				String resultVariable,
				DomainResultCreationState creationState) {
			// create SqlSelection based on the underlying JdbcMapping
			final SqlSelection sqlSelection = resolveSqlSelection(
					tableReference,
					keyColumn,
					creationState.getSqlAstCreationState()
			);

			// return a BasicResult with conversion the entity class or entity-name
			//noinspection unchecked,rawtypes
			return new BasicResult(
					sqlSelection.getValuesArrayPosition(),
					resultVariable,
					identifierPart.getJavaType(),
					null,
					navigablePath,
					false,
					!sqlSelection.isVirtual()
			);
		}
	}

	public static class CompositeKeyMapping extends AbstractKeyMapping {
		public CompositeKeyMapping(List<KeyColumn> keyColumns, EmbeddableValuedModelPart identifierPart) {
			super( keyColumns, identifierPart );
		}

		@Override
		public <K> DomainResult<K> createDomainResult(
				NavigablePath navigablePath,
				TableReference tableReference,
				String resultVariable,
				DomainResultCreationState creationState) {
			// this will be challenging if the embeddable defines to-ones.
			// just error for now.
			throw new UnsupportedOperationException( "Not implemented yet" );
		}
	}

	public static class KeyColumn implements TableDetails.KeyColumn {
		private final String tableName;
		private final String columnName;
		private final String writeExpression;

		private final boolean formula;

		private final JdbcMapping jdbcMapping;

		public KeyColumn(
				String tableName,
				String columnName,
				String writeExpression,
				boolean formula,
				JdbcMapping jdbcMapping) {
			this.tableName = tableName;
			this.columnName = columnName;
			this.writeExpression = writeExpression;
			this.formula = formula;
			this.jdbcMapping = jdbcMapping;
		}

		public String getColumnName() {
			return columnName;
		}

		@Override
		public String getContainingTableExpression() {
			return tableName;
		}

		@Override
		public String getWriteExpression() {
			return writeExpression;
		}

		@Override
		public String getSelectionExpression() {
			return columnName;
		}

		@Override
		public JdbcMapping getJdbcMapping() {
			return jdbcMapping;
		}

		@Override
		public boolean isFormula() {
			return formula;
		}

		@Override
		public boolean isNullable() {
			// keys are never nullable
			return false;
		}

		@Override
		public boolean isInsertable() {
			// keys are always insertable, unless this "column" is a formula
			return !formula;
		}

		@Override
		public boolean isUpdateable() {
			// keys are never updateable
			return false;
		}

		@Override
		public boolean isPartitioned() {
			return false;
		}

		@Override
		public @Nullable String getColumnDefinition() {
			return null;
		}

		@Override
		public @Nullable Long getLength() {
			return null;
		}

		@Override
		public @Nullable Integer getArrayLength() {
			return null;
		}

		@Override
		public @Nullable Integer getPrecision() {
			return null;
		}

		@Override
		public @Nullable Integer getScale() {
			return null;
		}

		@Override
		public @Nullable Integer getTemporalPrecision() {
			return null;
		}

		@Override
		public @Nullable String getCustomReadExpression() {
			return null;
		}

		@Override
		public @Nullable String getCustomWriteExpression() {
			return null;
		}
	}
}
