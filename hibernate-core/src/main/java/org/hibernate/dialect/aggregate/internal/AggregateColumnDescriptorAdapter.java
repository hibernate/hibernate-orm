/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.internal;

import java.util.List;
import org.hibernate.type.MappingContext;

import jakarta.annotation.Nullable;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.aggregate.spi.AggregateArrayElementDescriptor;
import org.hibernate.dialect.aggregate.spi.AggregateColumnDescriptor;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.StructuredJdbcType;

import static java.util.Objects.requireNonNull;

/**
 * @author Steve Ebersole
 */
public final class AggregateColumnDescriptorAdapter implements AggregateColumnDescriptor {
	private final Column column;
	private final MappingContext mappingContext;
	private final Namespace namespace;
	private final List<AggregateColumnDescriptor> components;

	private AggregateColumnDescriptorAdapter(Column column, Namespace namespace, MappingContext mappingContext) {
		this.column = column;
		this.mappingContext = mappingContext;
		this.namespace = namespace;
		this.components = column instanceof AggregateColumn aggregateColumn
				? descriptors( aggregateColumn.getComponent().getAggregatedColumns(), namespace, mappingContext )
				: List.of();
	}

	public static AggregateColumnDescriptorAdapter aggregate(
			AggregateColumn column,
			Namespace namespace, MappingContext mappingContext) {
		return new AggregateColumnDescriptorAdapter( column, namespace, mappingContext );
	}

	public static List<AggregateColumnDescriptor> descriptors(List<Column> columns, Namespace namespace, MappingContext mappingContext) {
		return columns.stream()
				.map( column -> (AggregateColumnDescriptor) new AggregateColumnDescriptorAdapter( column, namespace, mappingContext ) )
				.toList();
	}

	public static SqlTypedMapping mapping(Column column, MappingContext mappingContext) {
		return new ColumnMapping( column, mappingContext );
	}

	public static int effectiveSqlTypeCode(AggregateColumn column, MappingContext mappingContext) {
		final int jdbcTypeCode = column.getType( mappingContext ).getJdbcType().getDefaultSqlTypeCode();
		return jdbcTypeCode == SqlTypes.ARRAY ? column.getTypeCode() : jdbcTypeCode;
	}

	public static Column column(SqlTypedMapping mapping) {
		if ( mapping instanceof ColumnMapping columnMapping ) {
			return columnMapping.column;
		}
		throw new IllegalArgumentException( "Expected Hibernate aggregate column mapping adapter" );
	}

	public static AggregateColumn aggregateColumn(AggregateColumnDescriptor descriptor) {
		if ( descriptor instanceof AggregateColumnDescriptorAdapter adapter
				&& adapter.column instanceof AggregateColumn aggregateColumn ) {
			return aggregateColumn;
		}
		throw new IllegalArgumentException( "Expected Hibernate aggregate-column descriptor adapter" );
	}

	public static List<Column> columns(List<AggregateColumnDescriptor> descriptors) {
		return descriptors.stream()
				.map( descriptor -> ((AggregateColumnDescriptorAdapter) descriptor).column )
				.toList();
	}

	public static MappingContext mappingContext(AggregateColumnDescriptor descriptor) {
		return ((AggregateColumnDescriptorAdapter) descriptor).mappingContext;
	}

	public Namespace namespace() {
		return namespace;
	}

	@Override
	public String columnName() {
		return column.getName();
	}

	@Override
	public int sqlTypeCode() {
		return column instanceof AggregateColumn aggregateColumn
				? aggregateColumn.getTypeCode()
				: requireNonNull( column.getSqlTypeCode() );
	}

	@Override
	public String sqlTypeName() {
		return requireNonNull( column.getSqlType() );
	}

	@Override
	public SqlTypedMapping typeMapping() {
		return mapping( column, mappingContext );
	}

	@Override
	public boolean nullable() {
		return column.isNullable();
	}

	@Override
	public List<AggregateColumnDescriptor> components() {
		return components;
	}

	@Override
	public @Nullable AggregateArrayElementDescriptor arrayElement() {
		if ( !(column instanceof AggregateColumn aggregateColumn)
				|| aggregateColumn.getTypeCode() != SqlTypes.STRUCT_ARRAY
						&& aggregateColumn.getTypeCode() != SqlTypes.STRUCT_TABLE ) {
			return null;
		}
		final var jdbcType = (ArrayJdbcType) ((BasicType<?>) aggregateColumn.getValue().getType()).getJdbcType();
		final var elementJdbcType = (StructuredJdbcType) jdbcType.getElementJdbcType();
		return new AggregateArrayElementDescriptor(
				elementJdbcType.getStructTypeName(),
				elementJdbcType.getDefaultSqlTypeCode(),
				elementJdbcType.getDdlTypeCode(),
				aggregateColumn.getArrayLength() == null ? 127 : aggregateColumn.getArrayLength()
		);
	}

	private static final class ColumnMapping extends SqlTypedMappingImpl {
		private final Column column;

		private ColumnMapping(Column column, MappingContext mappingContext) {
			super(
					column.getLength(),
					column.getArrayLength(),
					column.getPrecision(),
					column.getScale(),
					column.getTemporalPrecision(),
					column.getType( mappingContext )
			);
			this.column = column;
		}
	}
}
