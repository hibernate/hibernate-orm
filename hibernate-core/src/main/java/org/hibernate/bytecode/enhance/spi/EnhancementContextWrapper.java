/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi;

import jakarta.persistence.metamodel.Type;

public class EnhancementContextWrapper implements EnhancementContext {

	private final ClassLoader loadingClassloader;
	private final EnhancementContext wrappedContext;

	public EnhancementContextWrapper(EnhancementContext wrappedContext, ClassLoader loadingClassloader) {
		this.wrappedContext = wrappedContext;
		this.loadingClassloader = loadingClassloader;
	}

	@Override
	public ClassLoader getLoadingClassLoader() {
		return loadingClassloader;
	}

	@Override
	public boolean isEntityClass(UnloadedClass classDescriptor) {
		return wrappedContext.isEntityClass( classDescriptor );
	}

	@Override
	public boolean isCompositeClass(UnloadedClass classDescriptor) {
		return wrappedContext.isCompositeClass( classDescriptor );
	}

	@Override
	public boolean isMappedSuperclassClass(UnloadedClass classDescriptor) {
		return wrappedContext.isMappedSuperclassClass( classDescriptor );
	}

	@Override
	public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
		return wrappedContext.doBiDirectionalAssociationManagement( field );
	}

	@Override
	public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
		return wrappedContext.doDirtyCheckingInline( classDescriptor );
	}

	@Override
	public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
		return wrappedContext.doExtendedEnhancement( classDescriptor );
	}

	@Override
	public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
		return wrappedContext.hasLazyLoadableAttributes( classDescriptor );
	}

	@Override
	public boolean isPersistentField(UnloadedField ctField) {
		return wrappedContext.isPersistentField( ctField );
	}

	@Override
	public UnloadedField[] order(UnloadedField[] persistentFields) {
		return wrappedContext.order( persistentFields );
	}

	@Override
	public boolean isLazyLoadable(UnloadedField field) {
		return wrappedContext.isLazyLoadable( field );
	}

	@Override
	public boolean isMappedCollection(UnloadedField field) {
		return wrappedContext.isMappedCollection( field );
	}

	@Override
	public boolean isDiscoveredType(UnloadedClass classDescriptor) {
		return wrappedContext.isDiscoveredType( classDescriptor );
	}

	@Override
	public void registerDiscoveredType(UnloadedClass classDescriptor, Type.PersistenceType type) {
		wrappedContext.registerDiscoveredType( classDescriptor, type );
	}
}
