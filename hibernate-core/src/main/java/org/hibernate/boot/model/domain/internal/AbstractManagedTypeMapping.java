/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractManagedTypeMapping implements ManagedTypeMappingImplementor {
	private static final Logger log = Logger.getLogger( AbstractManagedTypeMapping.class );

	private ManagedTypeMapping superTypeMapping;
	private TreeMap<String,PersistentAttributeMapping> declaredAttributeMappings;

	@Override
	public void addDeclaredPersistentAttribute(PersistentAttributeMapping attribute) {
		log.debugf(
				"Adding PersistentAttributeMapping [%s] to ManagedTypeMapping [%s]",
				attribute.getName(),
				getName()
		);

		if ( declaredAttributeMappings == null ) {
			declaredAttributeMappings = new TreeMap<>();
		}
		else {
			assert !declaredAttributeMappings.containsKey( attribute.getName() );
		}

		declaredAttributeMappings.put( attribute.getName(), attribute );
	}

	@Override
	public List<PersistentAttributeMapping> getDeclaredPersistentAttributes() {
		return declaredAttributeMappings == null
				? Collections.emptyList()
				: new ArrayList<>( declaredAttributeMappings.values() );
	}

	@Override
	public List<PersistentAttributeMapping> getPersistentAttributes() {
		List<PersistentAttributeMapping> attributes = new ArrayList<>();
		attributes.addAll( getDeclaredPersistentAttributes() );
		if ( getSuperManagedTypeMapping() != null ) {
			attributes.addAll( getSuperManagedTypeMapping().getPersistentAttributes() );
		}
		return attributes;
	}


	@Override
	public ManagedTypeMapping getSuperManagedTypeMapping() {
		return superTypeMapping;
	}

	@Override
	public void setSuperManagedType(ManagedTypeMapping superTypeMapping) {
		this.superTypeMapping = superTypeMapping;
	}

	@Override
	public List<ManagedTypeMapping> getSuperManagedTypeMappings() {
		List<ManagedTypeMapping> managedTypeMappings = new ArrayList<>();
		ManagedTypeMapping superType = superTypeMapping;
		while ( superType != null ) {
			managedTypeMappings.add( superType );
			superType = superType.getSuperManagedTypeMapping();
		}
		return managedTypeMappings;
	}
}
