/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi;

import javassist.CtClass;
import javassist.CtField;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
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
	public ClassLoader getLoadingClassLoader() {
		return getClass().getClassLoader();
	}

	/**
	 * look for @Entity annotation
	 */
	public boolean isEntityClass(CtClass classDescriptor) {
		return classDescriptor.hasAnnotation( Entity.class );
	}

	/**
	 * look for @Embeddable annotation
	 */
	public boolean isCompositeClass(CtClass classDescriptor) {
		return classDescriptor.hasAnnotation( Embeddable.class );
	}

	/**
	 * @return true
	 */
	public boolean doBiDirectionalAssociationManagement(CtField field) {
		return true;
	}

	/**
	 * @return true
	 */
	public boolean doDirtyCheckingInline(CtClass classDescriptor) {
		return true;
	}

	/**
	 * @return true
	 */
	public boolean hasLazyLoadableAttributes(CtClass classDescriptor) {
		return true;
	}

	/**
	 * @return true
	 */
	public boolean isLazyLoadable(CtField field) {
		return true;
	}

	/**
	 * look for @Transient annotation
	 */
	public boolean isPersistentField(CtField ctField) {
		return ! ctField.hasAnnotation( Transient.class );
	}

	/**
	 * look for @OneToMany, @ManyToMany and @ElementCollection annotations
	 */
	public boolean isMappedCollection(CtField field) {
		return field.hasAnnotation( OneToMany.class ) || field.hasAnnotation( ManyToMany.class )  || field.hasAnnotation( ElementCollection.class );
	}

	/**
	 * keep the same order.
	 */
	public CtField[] order(CtField[] persistentFields) {
		return persistentFields;
	}
}
