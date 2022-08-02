/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Collections;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.query.sqm.CastType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Models the type of a thing that can be used as an expression in a SQL query
 *
 * @author Steve Ebersole
 */
public interface JdbcMapping extends MappingType, JdbcMappingContainer {
	/**
	 * The descriptor for the Java type represented by this
	 * expressible type
	 */
	JavaType getJavaTypeDescriptor();

	/**
	 * The descriptor for the SQL type represented by this
	 * expressible type
	 */
	JdbcType getJdbcType();

	default CastType getCastType() {
		return getJdbcType().getCastType();
	}

	/**
	 * The strategy for extracting values of this expressible
	 * type from JDBC ResultSets, CallableStatements, etc
	 */
	ValueExtractor<?> getJdbcValueExtractor();

	/**
	 * The strategy for binding values of this expressible type to
	 * JDBC {@code PreparedStatement}s and {@code CallableStatement}s.
	 */
	ValueBinder getJdbcValueBinder();

	/**
	 * The strategy for formatting values of this expressible type to
	 * a SQL literal.
	 */
	@Incubating
	default JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return getJdbcType().getJdbcLiteralFormatter( getMappedJavaType() );
	}

	@Override
	default JavaType<?> getMappedJavaType() {
		return getJavaTypeDescriptor();
	}

	@Incubating
	default JavaType<?> getJdbcJavaType() {
		return getJavaTypeDescriptor();
	}

	/**
	 * Returns the converter that this basic type uses for transforming from the domain type, to the relational type,
	 * or <code>null</code> if there is no conversion.
	 */
	@Incubating
	default BasicValueConverter getValueConverter() {
		return null;
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
	default int forEachJdbcType(IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, this );
		return 1;
	}

	@Override
	default int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, this );
		return 1;
	}
}
