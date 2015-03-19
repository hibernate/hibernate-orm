/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
