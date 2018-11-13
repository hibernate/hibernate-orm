/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;

import net.bytebuddy.description.type.TypeDescription;

class ByteBuddyEnhancementContext {

	private final EnhancementContext enhancementContext;

	ByteBuddyEnhancementContext(EnhancementContext enhancementContext) {
		this.enhancementContext = enhancementContext;
	}

	public ClassLoader getLoadingClassLoader() {
		return enhancementContext.getLoadingClassLoader();
	}

	public boolean isEntityClass(TypeDescription classDescriptor) {
		return enhancementContext.isEntityClass( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean isCompositeClass(TypeDescription classDescriptor) {
		return enhancementContext.isCompositeClass( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean isMappedSuperclassClass(TypeDescription classDescriptor) {
		return enhancementContext.isMappedSuperclassClass( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean doDirtyCheckingInline(TypeDescription classDescriptor) {
		return enhancementContext.doDirtyCheckingInline( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean doExtendedEnhancement(TypeDescription classDescriptor) {
		return enhancementContext.doExtendedEnhancement( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean hasLazyLoadableAttributes(TypeDescription classDescriptor) {
		return enhancementContext.hasLazyLoadableAttributes( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean isPersistentField(AnnotatedFieldDescription field) {
		return enhancementContext.isPersistentField( field );
	}

	public AnnotatedFieldDescription[] order(AnnotatedFieldDescription[] persistentFields) {
		return (AnnotatedFieldDescription[]) enhancementContext.order( persistentFields );
	}

	public boolean isLazyLoadable(AnnotatedFieldDescription field) {
		return enhancementContext.isLazyLoadable( field );
	}

	public boolean isMappedCollection(AnnotatedFieldDescription field) {
		return enhancementContext.isMappedCollection( field );
	}

	public boolean doBiDirectionalAssociationManagement(AnnotatedFieldDescription field) {
		return enhancementContext.doBiDirectionalAssociationManagement( field );
	}
}
