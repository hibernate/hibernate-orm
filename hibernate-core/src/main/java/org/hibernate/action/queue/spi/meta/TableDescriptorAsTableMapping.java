/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.meta;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

import java.util.List;
import java.util.stream.Collectors;

/// Adapts a [TableDescriptor] to the [TableMapping] interface,
/// allowing graph-based infrastructure to use standard SQL model mutation builders.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating(since = "8.0", group = "action-queue")
public record TableDescriptorAsTableMapping(
		TableDescriptor descriptor,
		int relativePosition,
		boolean isIdentifierTable,
		boolean isInverse) implements TableMapping {

	@Nonnull
	@Override
	public String getTableName() {
		return descriptor.name();
	}


	@Override
	public boolean isOptional() {
		return descriptor.isOptional();
	}


	@Override
	public MutationDetails getInsertDetails() {
		return descriptor.insertDetails();
	}

	@Override
	public MutationDetails getUpdateDetails() {
		return descriptor.updateDetails();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return descriptor.cascadeDeleteEnabled();
	}

	@Override
	public MutationDetails getDeleteDetails() {
		return descriptor.deleteDetails();
	}


	@Nonnull
	@Override
	public KeyDetails getKeyDetails() {
		// Adapt TableKeyDescriptor to KeyDetails
		return new KeyDetailsAdapter( descriptor.keyDescriptor() );
	}

	/// Adapts TableKeyDescriptor to TableDetails.KeyDetails
	private static class KeyDetailsAdapter implements KeyDetails {
		private final TableKeyDescriptor keyDescriptor;
		private final List<KeyColumn> keyColumns;

		KeyDetailsAdapter(TableKeyDescriptor keyDescriptor) {
			this.keyDescriptor = keyDescriptor;
			// Wrap ColumnDescriptors as KeyColumns
			this.keyColumns = keyDescriptor.columns().stream()
					.map( KeyColumnAdapter::new )
					.collect( Collectors.toList() );
		}

		@Override
		public int getColumnCount() {
			return keyDescriptor.getJdbcTypeCount();
		}

		@Nonnull
		@Override
		public List<? extends KeyColumn> getKeyColumns() {
			return keyColumns;
		}

		@Nonnull
		@Override
		public KeyColumn getKeyColumn(int position) {
			return keyColumns.get( position );
		}

		@Override
		public void forEachKeyColumn(@Nonnull KeyColumnConsumer consumer) {
			for ( int i = 0; i < keyColumns.size(); i++ ) {
				consumer.consume( i, keyColumns.get( i ) );
			}
		}

		@Override
		public void breakDownKeyJdbcValues(
				@Nonnull Object domainValue,
				@Nonnull KeyValueConsumer valueConsumer,
				@Nonnull SharedSessionContractImplementor session) {
			// For simple keys, the domain value is the JDBC value
			// For composite keys, this would need more complex handling
			if ( keyColumns.size() == 1 ) {
				valueConsumer.consume( domainValue, keyColumns.get( 0 ) );
			}
			else {
				throw new UnsupportedOperationException( "Composite key breakdown not yet implemented" );
			}
		}

		@Nonnull
		@Override
		public <K> DomainResult<K> createDomainResult(
				@Nonnull NavigablePath navigablePath,
				@Nonnull TableReference tableReference,
				@Nullable String resultVariable,
				@Nonnull DomainResultCreationState creationState) {
			throw new UnsupportedOperationException( "Domain result creation not needed for mutations" );
		}

		@Override
		public int getJdbcTypeCount() {
			return keyDescriptor.getJdbcTypeCount();
		}

		@Nonnull
		@Override
		public SelectableMapping getSelectable(int columnIndex) {
			return keyDescriptor.getSelectable( columnIndex );
		}

		@Override
		public int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
			return keyDescriptor.forEachSelectable( offset, consumer );
		}
	}

	/// Adapts ColumnDescriptor to TableDetails.KeyColumn
	private static class KeyColumnAdapter implements KeyColumn {
		private final ColumnDescriptor columnDescriptor;

		KeyColumnAdapter(ColumnDescriptor columnDescriptor) {
			this.columnDescriptor = columnDescriptor;
		}

		@Nonnull
		@Override
		public String getColumnName() {
			return columnDescriptor.name();
		}

		@Nonnull
		@Override
		public JdbcMapping getJdbcMapping() {
			return columnDescriptor.jdbcMapping();
		}

		@Nonnull
		@Override
		public String getContainingTableExpression() {
			return columnDescriptor.getContainingTableExpression();
		}

		@Nonnull
		@Override
		public String getSelectionExpression() {
			return columnDescriptor.getSelectionExpression();
		}

		@Nullable
		@Override
		public String getCustomReadExpression() {
			return columnDescriptor.getCustomReadExpression();
		}

		@Nullable
		@Override
		public String getCustomWriteExpression() {
			return columnDescriptor.getCustomWriteExpression();
		}

		@Override
		public boolean isFormula() {
			return columnDescriptor.isFormula();
		}

		@Override
		public boolean isNullable() {
			return columnDescriptor.isNullable();
		}

		@Override
		public boolean isInsertable() {
			return columnDescriptor.isInsertable();
		}

		@Override
		public boolean isUpdateable() {
			return columnDescriptor.isUpdateable();
		}

		@Override
		public boolean isPartitioned() {
			return columnDescriptor.isPartitioned();
		}

		@Nullable
		@Override
		public Long getLength() {
			return columnDescriptor.getLength();
		}

		@Nullable
		@Override
		public Integer getPrecision() {
			return columnDescriptor.getPrecision();
		}

		@Nullable
		@Override
		public Integer getScale() {
			return columnDescriptor.getScale();
		}

		@Nullable
		@Override
		public Integer getTemporalPrecision() {
			return columnDescriptor.getTemporalPrecision();
		}

		@Nullable
		@Override
		public Integer getArrayLength() {
			return columnDescriptor.getArrayLength();
		}
	}
}
