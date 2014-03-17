/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.reflite.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Descriptor for the special java 'void' type.
 *
 * @author Steve Ebersole
 */
public class VoidDescriptor implements JavaTypeDescriptor {
	/**
	 * Singleton access
	 */
	public static final VoidDescriptor INSTANCE = new VoidDescriptor();

	public static final DotName NAME = DotName.createSimple( "void" );

	@Override
	public DotName getName() {
		return NAME;
	}

	@Override
	public int getModifiers() {
		return Void.class.getModifiers();
	}

	@Override
	public Collection<FieldDescriptor> getDeclaredFields() {
		return Collections.emptyList();
	}

	@Override
	public Collection<MethodDescriptor> getDeclaredMethods() {
		return Collections.emptyList();
	}

	@Override
	public AnnotationInstance findTypeAnnotation(DotName annotationType) {
		return null;
	}

	@Override
	public AnnotationInstance findLocalTypeAnnotation(DotName annotationType) {
		return null;
	}

	@Override
	public Collection<AnnotationInstance> findAnnotations(DotName annotationType) {
		return Collections.emptyList();
	}

	@Override
	public Collection<AnnotationInstance> findLocalAnnotations(DotName annotationType) {
		return Collections.emptyList();
	}

	@Override
	public boolean isAssignableFrom(JavaTypeDescriptor check) {
		if ( check == null ) {
			throw new IllegalArgumentException( "Descriptor to check cannot be null" );
		}

		return equals( check );
	}

	@Override
	public List<JavaTypeDescriptor> getResolvedParameterTypes() {
		return Collections.emptyList();
	}

	@Override
	public ClassInfo getJandexClassInfo() {
		return null;
	}
}
