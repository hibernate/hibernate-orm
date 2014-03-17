/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations.entity;

import javax.persistence.AccessType;

import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;

/**
 * Represents the information about an entity annotated with {@code @MappedSuperclass}.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class MappedSuperclassTypeMetadata extends IdentifiableTypeMetadata {
	/**
	 * This form is intended for cases where the MappedSuperclasses is part
	 * of the root super tree.
	 *
	 * @param javaTypeDescriptor The descriptor for the type annotated as MappedSuperclass
	 * @param defaultAccessType The default AccessType for he hierarchy
	 * @param context The binding context
	 */
	public MappedSuperclassTypeMetadata(
			ClassDescriptor javaTypeDescriptor,
			AccessType defaultAccessType,
			AnnotationBindingContext context) {
		super( javaTypeDescriptor, defaultAccessType, false, context );
	}

	/**
	 * This form is intended for cases where the MappedSuperclasses is part
	 * of the root subclass tree.
	 *
	 * @param javaTypeDescriptor The descriptor for the type annotated as MappedSuperclass
	 * @param superType The metadata representing the super type
	 * @param defaultAccessType The default AccessType for he hierarchy
	 * @param context The binding context
	 */
	public MappedSuperclassTypeMetadata(
			ClassDescriptor javaTypeDescriptor,
			IdentifiableTypeMetadata superType,
			AccessType defaultAccessType,
			AnnotationBindingContext context) {
		super( javaTypeDescriptor, superType, defaultAccessType, context );
	}
}


