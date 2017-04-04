/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.javassist;

import javassist.CtClass;
import javassist.CtField;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedField;

public class JavassistEnhancementContext {

	private final EnhancementContext enhancementContext;

	public JavassistEnhancementContext(EnhancementContext enhancementContext) {
		this.enhancementContext = enhancementContext;
	}

	public ClassLoader getLoadingClassLoader() {
		return enhancementContext.getLoadingClassLoader();
	}

	public boolean isEntityClass(CtClass classDescriptor) {
		return enhancementContext.isEntityClass( new UnloadedCtClass( classDescriptor ) );
	}

	public boolean isCompositeClass(CtClass classDescriptor) {
		return enhancementContext.isCompositeClass( new UnloadedCtClass( classDescriptor ) );
	}

	public boolean isMappedSuperclassClass(CtClass classDescriptor) {
		return enhancementContext.isMappedSuperclassClass( new UnloadedCtClass( classDescriptor ) );
	}

	public boolean doBiDirectionalAssociationManagement(CtField field) {
		return enhancementContext.doBiDirectionalAssociationManagement( new UnloadedCtField( field ) );
	}

	public boolean doDirtyCheckingInline(CtClass classDescriptor) {
		return enhancementContext.doDirtyCheckingInline( new UnloadedCtClass( classDescriptor ) );
	}

	public boolean doExtendedEnhancement(CtClass classDescriptor) {
		return enhancementContext.doExtendedEnhancement( new UnloadedCtClass( classDescriptor ) );
	}

	public boolean hasLazyLoadableAttributes(CtClass classDescriptor) {
		return enhancementContext.hasLazyLoadableAttributes( new UnloadedCtClass( classDescriptor ) );
	}

	public boolean isPersistentField(CtField ctField) {
		return enhancementContext.isPersistentField( new UnloadedCtField( ctField ) );
	}

	public CtField[] order(CtField[] persistentFields) {
		UnloadedField[] unloadedFields = new UnloadedField[persistentFields.length];
		for ( int i = 0; i < unloadedFields.length; i++ ) {
			unloadedFields[i] = new UnloadedCtField( persistentFields[i] );
		}
		UnloadedField[] ordered = enhancementContext.order( unloadedFields );
		CtField[] orderedFields = new CtField[persistentFields.length];
		for ( int i = 0; i < orderedFields.length; i++ ) {
			orderedFields[i] = ( (UnloadedCtField) ordered[i] ).ctField;
		}
		return orderedFields;
	}

	public boolean isLazyLoadable(CtField field) {
		return enhancementContext.isLazyLoadable( new UnloadedCtField( field ) );
	}

	public boolean isMappedCollection(CtField field) {
		return enhancementContext.isMappedCollection( new UnloadedCtField( field ) );
	}
}
