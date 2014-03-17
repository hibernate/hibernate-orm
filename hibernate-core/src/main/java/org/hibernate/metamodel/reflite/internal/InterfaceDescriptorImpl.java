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
 * @author Steve Ebersole
 */
public class InterfaceDescriptorImpl extends InternalJavaTypeDescriptor implements InterfaceDescriptor {
	private final ClassInfo classInfo;
	private final int modifiers;
	private final Map<DotName, AnnotationInstance> typeAnnotationMap;
	private final Map<DotName,List<AnnotationInstance>> annotationMap;

	private Collection<InterfaceDescriptor> extendedInterfaceTypes;

	private Collection<FieldDescriptor> fields;
	private Collection<MethodDescriptor> methods;
	private List<JavaTypeDescriptor> typeParameters;

	public InterfaceDescriptorImpl(
			ClassInfo classInfo,
			int modifiers,
			Map<DotName, AnnotationInstance> typeAnnotationMap,
			Map<DotName,List<AnnotationInstance>> annotationMap) {
		this.classInfo = classInfo;
		this.modifiers = modifiers;
		this.typeAnnotationMap = typeAnnotationMap != null
				? typeAnnotationMap
				: Collections.<DotName, AnnotationInstance>emptyMap();
		this.annotationMap = annotationMap != null
				? annotationMap
				: Collections.<DotName, List<AnnotationInstance>>emptyMap();
	}

	@Override
	public DotName getName() {
		return classInfo.name();
	}

	@Override
	public int getModifiers() {
		return modifiers;
	}

	@Override
	public Collection<InterfaceDescriptor> getExtendedInterfaceTypes() {
		return extendedInterfaceTypes;
	}

	@Override
	public AnnotationInstance findTypeAnnotation(DotName annotationType) {
		final AnnotationInstance localTypeAnnotation = findLocalTypeAnnotation( annotationType );
		if ( localTypeAnnotation != null ) {
			return localTypeAnnotation;
		}

		for ( InterfaceDescriptor extended : extendedInterfaceTypes ) {
			final AnnotationInstance annotationInstance = extended.findTypeAnnotation( annotationType );
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

		for ( InterfaceDescriptor extended : extendedInterfaceTypes ) {
			annotationInstances.addAll( extended.findAnnotations( annotationType ) );
		}

		return annotationInstances;
	}

	@Override
	public Collection<AnnotationInstance> findLocalAnnotations(DotName annotationType) {
		final Collection<AnnotationInstance> them = annotationMap.get( annotationType );
		return them == null ? Collections.<AnnotationInstance>emptyList() : them;
	}

	@Override
	public Collection<FieldDescriptor> getDeclaredFields() {
		return fields == null ? Collections.<FieldDescriptor>emptyList() : fields;
	}

	@Override
	public Collection<MethodDescriptor> getDeclaredMethods() {
		return methods == null ? Collections.<MethodDescriptor>emptyList() : methods;
	}

	@Override
	public String toString() {
		return "InterfaceDescriptorImpl{" + getName().toString() + '}';
	}

	void setExtendedInterfaceTypes(Collection<InterfaceDescriptor> extendedInterfaceTypes) {
		this.extendedInterfaceTypes = extendedInterfaceTypes;
	}

	void setFields(Collection<FieldDescriptor> fields) {
		this.fields = fields;
	}

	void setMethods(Collection<MethodDescriptor> methods) {
		this.methods = methods;
	}

	public List<JavaTypeDescriptor> getResolvedParameterTypes() {
		return typeParameters;
	}

	@Override
	public ClassInfo getJandexClassInfo() {
		return classInfo;
	}

	public void setTypeParameters(List<JavaTypeDescriptor> typeParameters) {
		this.typeParameters = typeParameters;
	}

	@Override
	public ClassDescriptor getSuperclass() {
		return null;
	}

	@Override
	public Collection<InterfaceDescriptor> getInterfaces() {
		return getExtendedInterfaceTypes();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final InterfaceDescriptorImpl that = (InterfaceDescriptorImpl) o;
		return this.getName().equals( that.getName() );
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}
