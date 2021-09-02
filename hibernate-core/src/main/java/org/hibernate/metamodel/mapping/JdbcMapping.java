/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Collections;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.query.CastType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * Models the type of a thing that can be used as an expression in a SQL query
 *
 * @author Steve Ebersole
 */
public interface JdbcMapping extends MappingType, JdbcMappingContainer {
	/**
	 * The descriptor for the Java type represented by this
	 * expressable type
	 */
	JavaTypeDescriptor getJavaTypeDescriptor();

	/**
	 * The descriptor for the SQL type represented by this
	 * expressable type
	 */
	JdbcTypeDescriptor getJdbcTypeDescriptor();

	default CastType getCastType() {
		return getJdbcTypeDescriptor().getCastType();
	}

	/**
	 * The strategy for extracting values of this expressable
	 * type from JDBC ResultSets, CallableStatements, etc
	 */
	ValueExtractor getJdbcValueExtractor(Dialect dialect);

	/**
	 * The strategy for binding values of this expressable
	 * type to JDBC PreparedStatements, CallableStatements, etc
	 */
	ValueBinder getJdbcValueBinder(Dialect dialect);

	@Override
	default JavaTypeDescriptor getMappedJavaTypeDescriptor() {
		return getJavaTypeDescriptor();
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
