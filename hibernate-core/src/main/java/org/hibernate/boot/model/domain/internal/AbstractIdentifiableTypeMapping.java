/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.spi.EntityMappingHierarchyImplementor;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractIdentifiableTypeMapping
		extends AbstractManagedTypeMapping
		implements IdentifiableTypeMappingImplementor {

	private static final Logger log = Logger.getLogger( AbstractIdentifiableTypeMapping.class );

	private final EntityMappingHierarchyImplementor entityMappingHierarchy;

	private IdentifiableTypeMappingImplementor superTypeMapping;
	private List<IdentifiableTypeMappingImplementor> subTypeMappings;

	public AbstractIdentifiableTypeMapping(EntityMappingHierarchyImplementor entityMappingHierarchy) {
		this.entityMappingHierarchy = entityMappingHierarchy;
	}

	@Override
	public void injectSuperclassMapping(IdentifiableTypeMappingImplementor superTypeMapping) {
		if ( this.superTypeMapping != null ) {
			log.debugf( "ManagedTypeMapping#injectSuperTypeMapping called multiple times" );
		}

		this.superTypeMapping = superTypeMapping;
		( (AbstractIdentifiableTypeMapping) superTypeMapping ).addSubclass( this );
	}

	private void addSubclass(AbstractIdentifiableTypeMapping subTypeMapping) {
		if ( subTypeMappings == null ) {
			subTypeMappings = new ArrayList<>();
		}
		subTypeMappings.add( subTypeMapping );
	}

	@Override
	public EntityMappingHierarchyImplementor getEntityMappingHierarchy() {
		return entityMappingHierarchy;
	}

	@Override
	public IdentifiableTypeMapping getSuperTypeMapping() {
		return superTypeMapping;
	}

	@Override
	public Collection<IdentifiableTypeMapping> getSubTypeMappings() {
		return subTypeMappings == null
				? Collections.emptyList()
				: Collections.unmodifiableList( subTypeMappings );
	}
}
