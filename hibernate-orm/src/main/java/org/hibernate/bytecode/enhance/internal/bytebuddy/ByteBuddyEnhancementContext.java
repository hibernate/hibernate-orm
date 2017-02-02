/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedField;

import net.bytebuddy.description.field.FieldDescription;
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

	public boolean doBiDirectionalAssociationManagement(FieldDescription field) {
		return enhancementContext.doBiDirectionalAssociationManagement( new UnloadedFieldDescription( field ) );
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

	public boolean isPersistentField(FieldDescription ctField) {
		return enhancementContext.isPersistentField( new UnloadedFieldDescription( ctField ) );
	}

	public FieldDescription[] order(FieldDescription[] persistentFields) {
		UnloadedField[] unloadedFields = new UnloadedField[persistentFields.length];
		for ( int i = 0; i < unloadedFields.length; i++ ) {
			unloadedFields[i] = new UnloadedFieldDescription( persistentFields[i] );
		}
		UnloadedField[] ordered = enhancementContext.order( unloadedFields );
		FieldDescription[] orderedFields = new FieldDescription[persistentFields.length];
		for ( int i = 0; i < orderedFields.length; i++ ) {
			orderedFields[i] = ( (UnloadedFieldDescription) ordered[i] ).fieldDescription;
		}
		return orderedFields;
	}

	public boolean isLazyLoadable(FieldDescription field) {
		return enhancementContext.isLazyLoadable( new UnloadedFieldDescription( field ) );
	}

	public boolean isMappedCollection(FieldDescription field) {
		return enhancementContext.isMappedCollection( new UnloadedFieldDescription( field ) );
	}
}
