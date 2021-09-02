/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Collections;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

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
	default JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return getMappedJavaTypeDescriptor();
	}

	@Override
	default JavaTypeDescriptor<T> getExpressableJavaTypeDescriptor() {
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

	@Override
	default List<JdbcMapping> getJdbcMappings() {
		return Collections.singletonList( this );
	}

	@Override
	default JavaTypeDescriptor<T> getMappedJavaTypeDescriptor() {
		return getJavaTypeDescriptor();
	}

	@Override
	default ValueExtractor<T> getJdbcValueExtractor(Dialect dialect) {
		return dialect.remapSqlTypeDescriptor( getJdbcTypeDescriptor() ).getExtractor( getMappedJavaTypeDescriptor() );
	}

	@Override
	default ValueBinder<T> getJdbcValueBinder(Dialect dialect) {
		return dialect.remapSqlTypeDescriptor( getJdbcTypeDescriptor() ).getBinder( getMappedJavaTypeDescriptor() );
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
	default int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	default int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
