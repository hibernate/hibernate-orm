/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Collections;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

/**
 * Marker interface for basic types.
 *
 * @author Steve Ebersole
 */
public interface BasicType<T> extends Type, BasicDomainType<T>, MappingType, BasicValuedMapping, JdbcMapping {
	/**
	 * Get the names under which this type should be registered in the type registry.
	 *
	 * @return The keys under which to register this type.
	 */
	String[] getRegistrationKeys();

	@Override
	default MappingType getMappedType() {
		return this;
	}

	@Override
	default JavaType<T> getJavaTypeDescriptor() {
		return this.getMappedJavaType();
	}

	@Override
	default JavaType<T> getExpressibleJavaType() {
		return getJavaTypeDescriptor();
	}

	@Override
	default int forEachJdbcType(IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, this );
		return 1;
	}

	@Override
	default JdbcMapping getJdbcMapping() {
		return this;
	}

	@Override
	default int getJdbcTypeCount() {
		return 1;
	}

	@Deprecated(forRemoval = true)
	@Override
	default List<JdbcMapping> getJdbcMappings() {
		return Collections.singletonList( this );
	}

	@Override
	default JdbcMapping getJdbcMapping(int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( index );
		}
		return this;
	}

	@Override
	default JdbcMapping getSingleJdbcMapping() {
		return this;
	}

	@Override
	default JavaType<T> getMappedJavaType() {
		return getJavaTypeDescriptor();
	}

	@Override
	@Incubating
	default BasicValueConverter<T, ?> getValueConverter() {
		return null;
	}

	@Override
	default ValueExtractor<T> getJdbcValueExtractor() {
		return getJdbcType().getExtractor( this.getMappedJavaType() );
	}

	@Override
	default ValueBinder<T> getJdbcValueBinder() {
		return getJdbcType().getBinder( this.getMappedJavaType() );
	}

	@Override
	default JdbcLiteralFormatter<T> getJdbcLiteralFormatter() {
		return getJdbcType().getJdbcLiteralFormatter( getMappedJavaType() );
	}

	@Override
	default int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	default Object disassemble(Object value, SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	default <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, x, y, value, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	/**
	 * The check constraint that should be added to the column
	 * definition in generated DDL.
	 *
	 * @param columnName the name of the column
	 * @param dialect the SQL {@link Dialect}
	 * @return a check constraint condition or null
	 * @since 6.2
	 */
	@Incubating
	default String getCheckCondition(String columnName, Dialect dialect) {
		final BasicValueConverter<T, ?> valueConverter = getValueConverter();
		String checkCondition = null;
		if ( valueConverter != null ) {
			checkCondition = valueConverter.getCheckCondition(
					columnName,
					getJdbcType(),
					dialect
			);
		}
		if ( checkCondition == null ) {
			checkCondition = getJdbcType().getCheckCondition(
					columnName,
					getMappedJavaType(),
					dialect
			);
		}
		return checkCondition;
	}

	@Incubating
	default String getSpecializedTypeDeclaration(Dialect dialect) {
		final BasicValueConverter<T, ?> valueConverter = getValueConverter();
		return valueConverter == null ? null : valueConverter.getSpecializedTypeDeclaration( getJdbcType(), dialect );
	}

	@Override
	default int compare(Object x, Object y, SessionFactoryImplementor sessionFactory) {
		return compare( x, y );
	}
}
