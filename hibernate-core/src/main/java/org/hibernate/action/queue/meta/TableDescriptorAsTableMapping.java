/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.hibernate.sql.model.TableMapping;

/**
 * Adapts a {@link TableDescriptor} to the {@link TableMapping} interface,
 * allowing graph-based infrastructure to use standard SQL model mutation builders.
 *
 * @author Steve Ebersole
 */
public class TableDescriptorAsTableMapping implements TableMapping {
	private final TableDescriptor descriptor;
	private final int relativePosition;
	private final boolean isIdentifierTable;
	private final boolean isInverse;

	public TableDescriptorAsTableMapping(
			TableDescriptor descriptor,
			int relativePosition,
			boolean isIdentifierTable,
			boolean isInverse) {
		this.descriptor = descriptor;
		this.relativePosition = relativePosition;
		this.isIdentifierTable = isIdentifierTable;
		this.isInverse = isInverse;
	}

	@Override
	public String getTableName() {
		return descriptor.name();
	}

	@Override
	public int getRelativePosition() {
		return relativePosition;
	}

	@Override
	public boolean isOptional() {
		return descriptor.isOptional();
	}

	@Override
	public boolean isInverse() {
		return isInverse;
	}

	@Override
	public boolean isIdentifierTable() {
		return isIdentifierTable;
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

	public TableDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public KeyDetails getKeyDetails() {
		// Adapt TableKeyDescriptor to KeyDetails
		return new KeyDetailsAdapter(descriptor.keyDescriptor());
	}

	/**
	 * Adapts TableKeyDescriptor to TableDetails.KeyDetails
	 */
	private static class KeyDetailsAdapter implements KeyDetails {
		private final TableKeyDescriptor keyDescriptor;
		private final java.util.List<KeyColumn> keyColumns;

		KeyDetailsAdapter(TableKeyDescriptor keyDescriptor) {
			this.keyDescriptor = keyDescriptor;
			// Wrap ColumnDescriptors as KeyColumns
			this.keyColumns = keyDescriptor.columns().stream()
					.map(KeyColumnAdapter::new)
					.collect(java.util.stream.Collectors.toList());
		}

		@Override
		public int getColumnCount() {
			return keyDescriptor.getJdbcTypeCount();
		}

		@Override
		public java.util.List<? extends KeyColumn> getKeyColumns() {
			return keyColumns;
		}

		@Override
		public KeyColumn getKeyColumn(int position) {
			return keyColumns.get(position);
		}

		@Override
		public void forEachKeyColumn(KeyColumnConsumer consumer) {
			for (int i = 0; i < keyColumns.size(); i++) {
				consumer.consume(i, keyColumns.get(i));
			}
		}

		@Override
		public void breakDownKeyJdbcValues(
				Object domainValue,
				KeyValueConsumer valueConsumer,
				org.hibernate.engine.spi.SharedSessionContractImplementor session) {
			// For simple keys, the domain value is the JDBC value
			// For composite keys, this would need more complex handling
			if (keyColumns.size() == 1) {
				valueConsumer.consume(domainValue, keyColumns.get(0));
			}
			else {
				throw new UnsupportedOperationException("Composite key breakdown not yet implemented");
			}
		}

		@Override
		public <K> org.hibernate.sql.results.graph.DomainResult<K> createDomainResult(
				org.hibernate.spi.NavigablePath navigablePath,
				org.hibernate.sql.ast.tree.from.TableReference tableReference,
				String resultVariable,
				org.hibernate.sql.results.graph.DomainResultCreationState creationState) {
			throw new UnsupportedOperationException("Domain result creation not needed for mutations");
		}

		@Override
		public int getJdbcTypeCount() {
			return keyDescriptor.getJdbcTypeCount();
		}

		@Override
		public org.hibernate.metamodel.mapping.SelectableMapping getSelectable(int columnIndex) {
			return keyDescriptor.getSelectable(columnIndex);
		}

		@Override
		public int forEachSelectable(int offset, org.hibernate.metamodel.mapping.SelectableConsumer consumer) {
			return keyDescriptor.forEachSelectable(offset, consumer);
		}
	}

	/**
	 * Adapts ColumnDescriptor to TableDetails.KeyColumn
	 */
	private static class KeyColumnAdapter implements KeyColumn {
		private final ColumnDescriptor columnDescriptor;

		KeyColumnAdapter(ColumnDescriptor columnDescriptor) {
			this.columnDescriptor = columnDescriptor;
		}

		@Override
		public String getColumnName() {
			return columnDescriptor.name();
		}

		@Override
		public org.hibernate.metamodel.mapping.JdbcMapping getJdbcMapping() {
			return columnDescriptor.jdbcMapping();
		}

		@Override
		public String getContainingTableExpression() {
			return columnDescriptor.getContainingTableExpression();
		}

		@Override
		public String getSelectionExpression() {
			return columnDescriptor.getSelectionExpression();
		}

		@Override
		public String getCustomReadExpression() {
			return columnDescriptor.getCustomReadExpression();
		}

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

		@Override
		public String getColumnDefinition() {
			return columnDescriptor.getColumnDefinition();
		}

		@Override
		public Long getLength() {
			return columnDescriptor.getLength();
		}

		@Override
		public Integer getPrecision() {
			return columnDescriptor.getPrecision();
		}

		@Override
		public Integer getScale() {
			return columnDescriptor.getScale();
		}

		@Override
		public Integer getTemporalPrecision() {
			return columnDescriptor.getTemporalPrecision();
		}

		@Override
		public Integer getArrayLength() {
			return columnDescriptor.getArrayLength();
		}
	}
}
