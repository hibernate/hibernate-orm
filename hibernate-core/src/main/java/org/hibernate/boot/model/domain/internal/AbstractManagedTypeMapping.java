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

import javax.persistence.metamodel.Type;

import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.domain.ManagedJavaTypeMapping;
import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.metamodel.model.domain.RepresentationMode;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractManagedTypeMapping implements ManagedTypeMappingImplementor {
	private static final Logger log = Logger.getLogger( AbstractManagedTypeMapping.class );

	private ManagedJavaTypeMapping javaTypeMapping;

	private ManagedTypeMapping superTypeMapping;
	private TreeMap<String, PersistentAttributeMapping> declaredAttributeMappings;


	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		return javaTypeMapping;
	}

	protected void setJavaTypeMapping(ManagedJavaTypeMapping javaTypeMapping) {
		this.javaTypeMapping = javaTypeMapping;
	}

	@Override
	public String getName() {
		return getJavaTypeMapping().getTypeName();
	}

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
	public PersistentAttributeMapping getDeclaredPersistentAttribute(String attributeName){
		return declaredAttributeMappings.get( attributeName );
	}

	@Override
	public boolean hasDeclaredPersistentAttribute(String name) {
		return declaredAttributeMappings.containsKey( name );
	}

	@Override
	public boolean hasPersistentAttribute(String name) {
		ManagedTypeMapping managedType = this;
		while ( managedType != null ) {
			if ( hasDeclaredPersistentAttribute( name ) ) {
				return true;
			}
			managedType = managedType.getSuperManagedTypeMapping();
		}
		return false;
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
	public void setExplicitRepresentationMode(RepresentationMode mode) {
		throw new UnsupportedOperationException( "Support for ManagedType-specific explicit RepresentationMode not yet implemented" );
	}

	@Override
	public RepresentationMode getExplicitRepresentationMode() {
		return null;
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

	/**
	 * Get the super managed type of this managed type if the super managed type is of the
	 * specified {@code persistenceType} or returns {@code null}.
	 *
	 * @param persistenceType The persistence type of interest.
	 * @return the super managed type mapping or null if the super type is not the same persistence type.
	 */
	protected ManagedTypeMapping getSuperManagedTypeMappingOfType(Type.PersistenceType persistenceType) {
		if ( superTypeMapping.getPersistenceType() == persistenceType ) {
			return superTypeMapping;
		}
		return null;
	}
}
