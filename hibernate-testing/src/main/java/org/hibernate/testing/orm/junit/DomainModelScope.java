/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.util.function.Consumer;

import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

/**
 * @author Steve Ebersole
 */
public interface DomainModelScope {
	MetadataImplementor getDomainModel();

	default void visitHierarchies(Consumer<RootClass> action) {
		getDomainModel().getEntityBindings().forEach(
				persistentClass -> {
					if ( persistentClass instanceof RootClass ) {
						action.accept( (RootClass) persistentClass );
					}
				}
		);
	}

	default void withHierarchy(Class<?> rootType, Consumer<RootClass> action) {
		withHierarchy( rootType.getName(), action );
	}

	default void withHierarchy(String rootTypeName, Consumer<RootClass> action) {
		final PersistentClass entityBinding = getDomainModel().getEntityBinding( rootTypeName );
		if ( entityBinding == null ) {
			throw new UnknownEntityTypeException( rootTypeName );
		}

		action.accept( entityBinding.getRootClass() );
	}

	default PersistentClass getEntityBinding(Class<?> theEntityClass) {
		assert theEntityClass != null;
		return getDomainModel().getEntityBinding( theEntityClass.getName() );
	}


	// ...
}
