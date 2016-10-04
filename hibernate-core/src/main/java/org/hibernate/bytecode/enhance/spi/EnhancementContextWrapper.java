/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi;

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
}
