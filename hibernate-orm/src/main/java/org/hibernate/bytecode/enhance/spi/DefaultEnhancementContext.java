/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 * default implementation of EnhancementContext. May be sub-classed as needed.
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DefaultEnhancementContext implements EnhancementContext {

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
		return classDescriptor.hasAnnotation( Embeddable.class );
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
		return field.hasAnnotation( OneToMany.class ) || field.hasAnnotation( ManyToMany.class )  || field.hasAnnotation( ElementCollection.class );
	}

	/**
	 * keep the same order.
	 */
	@Override
	public UnloadedField[] order(UnloadedField[] persistentFields) {
		return persistentFields;
	}
}
