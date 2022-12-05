/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.function.Consumer;

import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.persister.entity.AttributeMappingsList;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Commonality in regards to the mapping type system for all managed domain
 * types - entity types, mapped-superclass types, composite types, etc
 *
 * @author Steve Ebersole
 */
public interface ManagedMappingType extends MappingType, FetchableContainer {
	@Override
	default JavaType<?> getJavaType() {
		return getMappedJavaType();
	}

	@Override
	default MappingType getPartMappingType() {
		return this;
	}

	/**
	 * Get the number of attributes defined on this class and any supers
	 */
	int getNumberOfAttributeMappings();

	/**
	 * Retrieve an attribute by its contributor position
	 */
	AttributeMapping getAttributeMapping(int position);

	default AttributeMapping findAttributeMapping(String name) {
		return null;
	}

	/**
	 * Get access to the attributes defined on this class and any supers
	 */
	AttributeMappingsList getAttributeMappings();

	/**
	 * Visit attributes defined on this class and any supers
	 */
	void visitAttributeMappings(Consumer<? super AttributeMapping> action);

	/**
	 * Visit attributes defined on this class and any supers
	 */
	default void forEachAttributeMapping(IndexedConsumer<AttributeMapping> consumer) {
		getAttributeMappings().forEachAttributeMapping( consumer );
	}

	Object[] getValues(Object instance);

	default Object getValue(Object instance, int position) {
		return getAttributeMapping( position ).getValue( instance );
	}

	void setValues(Object instance, Object[] resolvedValues);

	default void setValue(Object instance, int position, Object value) {
		getAttributeMapping( position ).setValue( instance, value );
	}

}
