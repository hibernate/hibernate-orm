/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.persistence.metamodel.Type;

/**
 * default implementation of EnhancementContext. May be sub-classed as needed.
 *
 * @author Luis Barreiro
 */
public class DefaultEnhancementContext implements EnhancementContext {

	private final ConcurrentHashMap<String, Type.PersistenceType> discoveredTypes = new ConcurrentHashMap<>();

	/**
	 * @return the classloader for this class
	 */
	@Override
	public ClassLoader getLoadingClassLoader() {
		return getClass().getClassLoader();
	}

	/**
	 * look for @Entity annotation
	 */
	@Override
	public boolean isEntityClass(UnloadedClass classDescriptor) {
		return classDescriptor.hasAnnotation( Entity.class );
	}

	/**
	 * look for @Embeddable annotation
	 */
	@Override
	public boolean isCompositeClass(UnloadedClass classDescriptor) {
		return classDescriptor.hasAnnotation( Embeddable.class )
				|| discoveredTypes.get( classDescriptor.getName() ) == Type.PersistenceType.EMBEDDABLE;
	}

	/**
	 * look for @MappedSuperclass annotation
	 */
	@Override
	public boolean isMappedSuperclassClass(UnloadedClass classDescriptor) {
		return classDescriptor.hasAnnotation( MappedSuperclass.class );
	}

	/**
	 * @return true
	 */
	@Override
	public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
		return true;
	}

	/**
	 * @return true
	 */
	@Override
	public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
		return true;
	}

	/**
	 * @return false
	 */
	@Override
	public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
		return false;
	}

	/**
	 * @return true
	 */
	@Override
	public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
		return true;
	}

	/**
	 * @return true
	 */
	@Override
	public boolean isLazyLoadable(UnloadedField field) {
		return true;
	}

	/**
	 * look for @Transient annotation
	 */
	@Override
	public boolean isPersistentField(UnloadedField ctField) {
		return ! ctField.hasAnnotation( Transient.class );
	}

	/**
	 * look for @OneToMany, @ManyToMany and @ElementCollection annotations
	 */
	@Override
	public boolean isMappedCollection(UnloadedField field) {
		// If the collection is definitely a plural attribute, we respect that
		if (field.hasAnnotation( OneToMany.class ) || field.hasAnnotation( ManyToMany.class ) || field.hasAnnotation( ElementCollection.class )) {
			return true;
		}
		// But a collection might be treated like a singular attribute if it is annotated with `@Basic`
		// If no annotations are given though, a collection is treated like a OneToMany
		return !field.hasAnnotation( Basic.class );
	}

	/**
	 * keep the same order.
	 */
	@Override
	public UnloadedField[] order(UnloadedField[] persistentFields) {
		return persistentFields;
	}

	@Override
	public boolean isDiscoveredType(UnloadedClass classDescriptor) {
		return discoveredTypes.containsKey( classDescriptor.getName() );
	}

	@Override
	public void registerDiscoveredType(UnloadedClass classDescriptor, Type.PersistenceType type) {
		discoveredTypes.put( classDescriptor.getName(), type );
	}
}
