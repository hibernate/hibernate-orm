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
package org.hibernate.metamodel.reflite.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.InterfaceDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Implementation of a type descriptor
 *
 * @author Steve Ebersole
 */
public class ClassDescriptorImpl extends InternalJavaTypeDescriptor implements ClassDescriptor {
	private final ClassInfo jandexClassInfo;

	private final int modifiers;
	private final boolean hasDefaultConstructor;
	private final Map<DotName,AnnotationInstance> typeAnnotationMap;
	private final Map<DotName,List<AnnotationInstance>> annotationMap;

	private ClassDescriptor superType;
	private Collection<InterfaceDescriptor> interfaces;

	private Collection<FieldDescriptor> fieldDescriptors;
	private Collection<MethodDescriptor> methodDescriptors;
	private List<JavaTypeDescriptor> typeParameters;

	public ClassDescriptorImpl(
			ClassInfo jandexClassInfo,
			int modifiers,
			boolean hasDefaultConstructor,
			Map<DotName, AnnotationInstance> typeAnnotationMap,
			Map<DotName, List<AnnotationInstance>> annotationMap) {
		this.jandexClassInfo = jandexClassInfo;
		this.modifiers = modifiers;
		this.hasDefaultConstructor = hasDefaultConstructor;
		this.typeAnnotationMap = typeAnnotationMap != null
				? typeAnnotationMap
				: Collections.<DotName, AnnotationInstance>emptyMap();
		this.annotationMap = annotationMap != null
				? annotationMap
				: Collections.<DotName, List<AnnotationInstance>>emptyMap();
	}

	@Override
	public DotName getName() {
		return jandexClassInfo.name();
	}

	@Override
	public int getModifiers() {
		return modifiers;
	}

	@Override
	public ClassDescriptor getSuperType() {
		return superType;
	}

	@Override
	public Collection<InterfaceDescriptor> getInterfaceTypes() {
		return interfaces;
	}

	@Override
	public AnnotationInstance findTypeAnnotation(DotName annotationType) {
		final AnnotationInstance localTypeAnnotation = findLocalTypeAnnotation( annotationType );
		if ( localTypeAnnotation != null ) {
			return localTypeAnnotation;
		}

		if ( superType != null ) {
			return superType.findTypeAnnotation( annotationType );
		}

		for ( InterfaceDescriptor interfaceDescriptor : interfaces ) {
			final AnnotationInstance annotationInstance = interfaceDescriptor.findTypeAnnotation( annotationType );
			if ( annotationInstance != null ) {
				return annotationInstance;
			}
		}

		return null;
	}

	@Override
	public AnnotationInstance findLocalTypeAnnotation(DotName annotationType) {
		return typeAnnotationMap.get( annotationType );
	}

	@Override
	public Collection<AnnotationInstance> findAnnotations(DotName annotationType) {
		final List<AnnotationInstance> annotationInstances = new ArrayList<AnnotationInstance>();

		annotationInstances.addAll( findLocalAnnotations( annotationType ) );

		if ( superType != null ) {
			annotationInstances.addAll( superType.findAnnotations( annotationType ) );
		}

		for ( InterfaceDescriptor interfaceDescriptor : interfaces ) {
			annotationInstances.addAll( interfaceDescriptor.findAnnotations( annotationType ) );
		}

		return annotationInstances;
	}

	@Override
	public Collection<AnnotationInstance> findLocalAnnotations(DotName annotationType) {
		final Collection<AnnotationInstance> them = annotationMap.get( annotationType );
		return them == null ? Collections.<AnnotationInstance>emptyList() : them;
	}

//	@Override
//	public boolean isAssignableFrom(JavaTypeDescriptor check) {
//		if ( check == null ) {
//			throw new IllegalArgumentException( "Descriptor to check cannot be null" );
//		}
//
//		if ( equals( check ) ) {
//			return true;
//		}
//
//		if ( superType != null && superType.isAssignableFrom( check ) ) {
//			return true;
//		}
//
//		for ( InterfaceDescriptor implementsInterface : getInterfaceTypes() ) {
//			if ( implementsInterface.isAssignableFrom( check ) ) {
//				return true;
//			}
//		}
//
//		return false;
//	}
//
	@Override
	public boolean hasDefaultConstructor() {
		return hasDefaultConstructor;
	}

	@Override
	public Collection<FieldDescriptor> getDeclaredFields() {
		return fieldDescriptors == null ? Collections.<FieldDescriptor>emptyList() : fieldDescriptors;
	}

	@Override
	public Collection<MethodDescriptor> getDeclaredMethods() {
		return methodDescriptors == null ? Collections.<MethodDescriptor>emptyList() : methodDescriptors;
	}

	@Override
	public ClassInfo getJandexClassInfo() {
		return jandexClassInfo;
	}

	@Override
	public String toString() {
		return "ClassDescriptorImpl{" + getName().toString() + '}';
	}

	void setSuperType(ClassDescriptor superType) {
		this.superType = superType;
	}

	void setInterfaces(Collection<InterfaceDescriptor> interfaces) {
		this.interfaces = interfaces;
	}

	void setFields(Collection<FieldDescriptor> fieldDescriptors) {
		this.fieldDescriptors = fieldDescriptors;
	}

	void setMethods(Collection<MethodDescriptor> methodDescriptors) {
		this.methodDescriptors = methodDescriptors;
	}

	public void setTypeParameters(List<JavaTypeDescriptor> typeParameters) {
		this.typeParameters = typeParameters;
	}

	@Override
	public List<JavaTypeDescriptor> getResolvedParameterTypes() {
		return typeParameters;
	}
	@Override
	public ClassDescriptor getSuperclass() {
		return null;
	}

	@Override
	public Collection<InterfaceDescriptor> getInterfaces() {
		return getInterfaceTypes();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ClassDescriptorImpl that = (ClassDescriptorImpl) o;
		return getName().equals( that.getName() );
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}
