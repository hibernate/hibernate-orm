/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.mapping.IndexedConsumer;
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
	default JavaType<?> getJavaTypeDescriptor() {
		return getMappedJavaTypeDescriptor();
	}

	@Override
	default MappingType getPartMappingType() {
		return this;
	}

	/**
	 * Get the number of attributes defined on this class and any supers
	 */
	int getNumberOfAttributeMappings();

	default AttributeMapping findAttributeMapping(String name) {
		return null;
	}

	/**
	 * Get access to the attributes defined on this class and any supers
	 */
	List<AttributeMapping> getAttributeMappings();

	/**
	 * Visit attributes defined on this class and any supers
	 */
	void visitAttributeMappings(Consumer<? super AttributeMapping> action);

	/**
	 * Visit attributes defined on this class and any supers
	 */
	default void forEachAttributeMapping(IndexedConsumer<AttributeMapping> consumer) {
		final List<AttributeMapping> attributeMappings = getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			consumer.accept( i, attributeMappings.get( i ) );
		}
	}

	/**
	 * @todo (6.0) : consider dropping this in favor of a form passing the ManagedMappingType
	 * 		which indicates the type to limit the attribute search to (the type and its super-type)
	 */
	default void visitStateArrayContributors(Consumer<StateArrayContributorMapping> mappingConsumer) {
		visitAttributeMappings(
				modelPart -> {
					if ( modelPart instanceof StateArrayContributorMapping ) {
						mappingConsumer.accept( ( (StateArrayContributorMapping) modelPart ) );
					}
				}
		);
	}
}
