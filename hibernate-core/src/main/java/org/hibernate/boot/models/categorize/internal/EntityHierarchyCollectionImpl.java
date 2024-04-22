/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityHierarchyCollection;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Steve Ebersole
 */
public class EntityHierarchyCollectionImpl implements EntityHierarchyCollection {
	private final Map<ClassDetails, EntityHierarchy> hierarchyByRoot = new LinkedHashMap<>();
	private final Map<ClassDetails, EntityHierarchy> hierarchyByAbsoluteRoot = new LinkedHashMap<>();


	public EntityHierarchyCollectionImpl(Collection<EntityHierarchy> hierarchies) {
		hierarchies.forEach( (hierarchy) -> {
			hierarchyByRoot.put( hierarchy.getRoot().getClassDetails(), hierarchy );
			hierarchyByAbsoluteRoot.put( hierarchy.getAbsoluteRoot().getClassDetails(), hierarchy );
		} );
	}

	@Override
	public Collection<EntityHierarchy> getHierarchies() {
		return hierarchyByRoot.values();
	}

	@Override
	public void forEachHierarchy(Consumer<EntityHierarchy> consumer) {
		hierarchyByRoot.values().forEach( consumer );
	}

	@Override
	public EntityHierarchy determineEntityHierarchy(ClassDetails classToLookFor) {
		assert classToLookFor.hasAnnotationUsage( Entity.class )
				|| classToLookFor.hasAnnotationUsage( MappedSuperclass.class );

		final EntityHierarchy asRoot = hierarchyByRoot.get( classToLookFor );
		if ( asRoot != null ) {
			return asRoot;
		}

		final EntityHierarchy asAbsoluteRoot = hierarchyByAbsoluteRoot.get( classToLookFor );
		if ( asAbsoluteRoot != null ) {
			return asAbsoluteRoot;
		}

		for ( Map.Entry<ClassDetails, EntityHierarchy> entry : hierarchyByRoot.entrySet() ) {
			// NOTE : we know it is not the root nor absolute root so we can skip those
			final EntityTypeMetadata hierarchyRoot = entry.getValue().getRoot();
			if ( hierarchyRoot.isSubType( classToLookFor ) ) {
				return entry.getValue();
			}
		}

		throw new ModelsException( "Could not determine hierarchy of which `" + classToLookFor.getName() + "` is part" );
	}

}
