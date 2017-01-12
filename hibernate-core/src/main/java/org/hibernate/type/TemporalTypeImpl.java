/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Comparator;

import org.hibernate.MappingException;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.ColumnMapping;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.TemporalType;
import org.hibernate.type.spi.basic.AbstractTemporalTypeImpl;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.spi.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.TemporalTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * An AbstractTemporalTypeImpl implementation that, much like {@link BasicTypeImpl}, is
 * designed based on construction binding of the delegates
 *
 * @author Steve Ebersole
 */
public class TemporalTypeImpl<T> extends AbstractTemporalTypeImpl<T> implements TemporalType<T> {
	private final javax.persistence.TemporalType precision;
	private final ColumnMapping[] columnMapping;
	private final TemporalTypeDescriptor<T> domainJavaType;

	private final MutabilityPlan<T> mutabilityPlan;
	private final Comparator<T> comparator;

	private final AttributeConverterDefinition<T,?> converterDefinition;

	private final JdbcLiteralFormatter<T> jdbcLiteralFormatter;

	private final int[] sqlTypes;

	public TemporalTypeImpl(TemporalTypeDescriptor<T> domainJavaType, SqlTypeDescriptor sqlType) {
		this( domainJavaType.getPrecision(), domainJavaType, sqlType );
	}

	public TemporalTypeImpl(
			javax.persistence.TemporalType precision,
			TemporalTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType) {
		this( precision, domainJavaType, sqlType, domainJavaType.getMutabilityPlan(), domainJavaType.getComparator(), null );
	}

	public TemporalTypeImpl(
			TemporalTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator) {
		this( domainJavaType.getPrecision(), domainJavaType, sqlType, mutabilityPlan, comparator, null );
	}

	public TemporalTypeImpl(
			javax.persistence.TemporalType precision,
			TemporalTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator) {
		this( precision, domainJavaType, sqlType, mutabilityPlan, comparator, null );
	}

	public TemporalTypeImpl(
			TemporalTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator,
			AttributeConverterDefinition<T,?> converterDefinition) {
		this( domainJavaType.getPrecision(), domainJavaType, sqlType, mutabilityPlan, comparator, converterDefinition );
	}

	public TemporalTypeImpl(
			javax.persistence.TemporalType precision,
			TemporalTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator,
			AttributeConverterDefinition<T,?> converterDefinition) {
		this(
				precision,
				domainJavaType,
				sqlType,
				mutabilityPlan,
				comparator,
				converterDefinition,
				sqlType.getJdbcLiteralFormatter( domainJavaType )
		);
	}

	public TemporalTypeImpl(
			javax.persistence.TemporalType precision,
			TemporalTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator,
			AttributeConverterDefinition<T,?> converterDefinition,
			JdbcLiteralFormatter<T> jdbcLiteralFormatter) {
		this.precision = precision;
		this.domainJavaType = domainJavaType;
		this.columnMapping = new ColumnMapping[] {new ColumnMapping( sqlType )};
		this.mutabilityPlan = mutabilityPlan;
		this.comparator = comparator;
		this.converterDefinition = converterDefinition;
		this.jdbcLiteralFormatter = jdbcLiteralFormatter;
		this.sqlTypes = new int[] { sqlType.getSqlType() };
	}

	@Override
	public TemporalTypeDescriptor<T> getJavaTypeDescriptor() {
		return domainJavaType;
	}

	@Override
	public javax.persistence.TemporalType getPrecision() {
		return precision;
	}

	@Override
	public MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public Comparator<T> getComparator() {
		return comparator;
	}

	@Override
	public ColumnMapping[] getColumnMappings() {
		return columnMapping;
	}

	@Override
	public AttributeConverterDefinition<T, ?> getAttributeConverterDefinition() {
		return converterDefinition;
	}

	@Override
	public JdbcLiteralFormatter<T> getJdbcLiteralFormatter() {
		return jdbcLiteralFormatter;
	}

	@Override
	public int[] sqlTypes() throws MappingException {
		return sqlTypes;
	}

	@Override
	public String asLoggableText() {
		return null;
	}
}
