/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Steve Ebersole
 */
public interface DomainModelScope {
	MetadataImplementor getDomainModel();

	default void visitHierarchies(Consumer<EntityMappingHierarchy> action) {
		getDomainModel().getEntityHierarchies().forEach( action );
	}

	default void withHierarchy(Class rootType, Consumer<EntityMappingHierarchy> action) {
		withHierarchy( rootType.getName(), action );
	}

	default void withHierarchy(String rootTypeName, Consumer<EntityMappingHierarchy> action) {
		final PersistentClass entityBinding = getDomainModel().getEntityBinding( rootTypeName );

		if ( entityBinding == null ) {
			throw new UnknownEntityTypeException(
					String.format(
							Locale.ROOT,
							"Could not resolve `%s` as an entity type",
							rootTypeName
					)
			);
		}

		action.accept( entityBinding.getEntityMappingHierarchy() );
	}


	// ...
}
