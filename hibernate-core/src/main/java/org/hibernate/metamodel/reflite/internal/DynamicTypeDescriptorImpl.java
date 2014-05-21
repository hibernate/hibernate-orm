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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.metamodel.reflite.spi.DynamicAttributeDescriptor;
import org.hibernate.metamodel.reflite.spi.DynamicTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.reflite.spi.ParameterizedType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Standard implementation of the DynamicTypeDescriptor contract.
 *
 * @author Steve Ebersole
 */
public class DynamicTypeDescriptorImpl implements DynamicTypeDescriptor, AnnotationTarget {
	private final DotName name;
	private DynamicTypeDescriptorImpl superType;
	private DynamicAnnotationBuilder annotationBuilder;

	private Collection<DynamicAttributeDescriptor> attributes;

	private Map<DotName, AnnotationInstance> typeAnnotationMap;
	private Map<DotName,Collection<AnnotationInstance>> classAnnotationMap;

	public DynamicTypeDescriptorImpl(DotName name, DynamicTypeDescriptorImpl superType) {
		this.name = name;
		this.superType = superType;
	}

	public void setSuperType(DynamicTypeDescriptorImpl superType) {
		if ( this.superType != null ) {
			throw new IllegalStateException( "Dynamic type super-type already set" );
		}
		this.superType = superType;
	}

	/**
	 * Makes (and returns) a member definition as declared on this dynamic type.
	 *
	 * @param name The name of the member
	 * @param type The member type
	 */
	public DynamicAttributeDescriptor makeAttribute(String name, JavaTypeDescriptor type) {
		return makeAttribute(
				name,
				new ParameterizedTypeImpl( type, Collections.<JavaTypeDescriptor>emptyList() )
		);
	}

	/**
	 * Makes (and returns) a member definition as declared on this dynamic type.
	 *
	 * @param name The name of the member
	 * @param type The member type
	 */
	public DynamicAttributeDescriptor makeAttribute(String name, ParameterizedType type) {
		if ( attributes == null ) {
			attributes = new ArrayList<DynamicAttributeDescriptor>();
		}

		final DynamicAttributeDescriptorImpl attributeDescriptor = new DynamicAttributeDescriptorImpl( this, name, type );
		attributes.add( attributeDescriptor );

		return attributeDescriptor;
	}

	public DynamicAnnotationBuilder getAnnotationBuilder() {
		if ( annotationBuilder == null ) {
			annotationBuilder = new DynamicTypeAnnotationBuilder( this );
		}
		return annotationBuilder;
	}

	@Override
	public DynamicTypeDescriptorImpl getSuperType() {
		return superType;
	}

	@Override
	public Collection<DynamicAttributeDescriptor> getDeclaredAttributes() {
		return attributes == null ? Collections.<DynamicAttributeDescriptor>emptyList() : attributes;
	}

	@Override
	public DotName getName() {
		return name;
	}

	@Override
	public int getModifiers() {
		return Modifier.PUBLIC;
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
	public List<JavaTypeDescriptor> getResolvedParameterTypes() {
		return Collections.emptyList();
	}

	@Override
	public ClassInfo getJandexClassInfo() {
		throw new UnsupportedOperationException(
				"Illegal attempt to get Jandex ClassInfo from DynamicTypeDescriptor"
		);
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

		return null;
	}

	@Override
	public AnnotationInstance findLocalTypeAnnotation(DotName annotationType) {
		return typeAnnotationMap == null ? null : typeAnnotationMap.get( annotationType );
	}

	@Override
	public Collection<AnnotationInstance> findAnnotations(DotName annotationType) {
		final List<AnnotationInstance> annotationInstances = new ArrayList<AnnotationInstance>();

		annotationInstances.addAll( findLocalAnnotations( annotationType ) );

		if ( superType != null ) {
			annotationInstances.addAll( superType.findAnnotations( annotationType ) );
		}

		return annotationInstances;
	}

	@Override
	public Collection<AnnotationInstance> findLocalAnnotations(DotName annotationType) {
		final Collection<AnnotationInstance> them = classAnnotationMap.get( annotationType );
		return them == null ? Collections.<AnnotationInstance>emptyList() : them;
	}

	private static class DynamicTypeAnnotationBuilder implements DynamicAnnotationBuilder {
		private final DynamicTypeDescriptorImpl type;

		private DynamicTypeAnnotationBuilder(DynamicTypeDescriptorImpl type) {
			this.type = type;
		}

		@Override
		public AnnotationInstance makeAnnotation(DotName annotationTypeName, AnnotationValue... values) {
			final AnnotationInstance annotationInstance = AnnotationInstance.create( annotationTypeName, type, values );
			return registerAnnotation( annotationInstance );
		}

		private AnnotationInstance registerAnnotation(AnnotationInstance annotationInstance) {
			type.addTypeAnnotation( annotationInstance );
			type.addClassAnnotation( annotationInstance );

			return annotationInstance;
		}

		@Override
		public AnnotationInstance makeAnnotation(DotName annotationTypeName, List<AnnotationValue> values) {
			final AnnotationInstance annotationInstance = AnnotationInstance.create( annotationTypeName, type, values );
			return registerAnnotation( annotationInstance );
		}
	}

	void addClassAnnotation(AnnotationInstance annotationInstance) {
		Collection<AnnotationInstance> annotationInstances;
		if ( classAnnotationMap == null ) {
			classAnnotationMap = new HashMap<DotName, Collection<AnnotationInstance>>();
			annotationInstances = new ArrayList<AnnotationInstance>();
			classAnnotationMap.put( annotationInstance.name(), annotationInstances );
		}
		else {
			annotationInstances = classAnnotationMap.get( annotationInstance.name() );
			if ( annotationInstances == null ) {
				annotationInstances = new ArrayList<AnnotationInstance>();
				classAnnotationMap.put( annotationInstance.name(), annotationInstances );
			}
		}
		annotationInstances.add( annotationInstance );
	}

	private void addTypeAnnotation(AnnotationInstance annotationInstance) {
		if ( typeAnnotationMap == null ) {
			typeAnnotationMap = new HashMap<DotName, AnnotationInstance>();
		}

		final AnnotationInstance old = typeAnnotationMap.put( annotationInstance.name(), annotationInstance );
		if ( old != null ) {
			throw new IllegalAnnotationException(
					String.format(
							Locale.ENGLISH,
							"%s already contained a type annotation instance for %s : %s",
							getName().toString(),
							annotationInstance.name().toString(),
							old
					)
			);
		}
	}


	@Override
	public boolean isAssignableFrom(JavaTypeDescriptor check) {
		if ( check == null ) {
			throw new IllegalArgumentException( "Descriptor to check cannot be null" );
		}

		if ( equals( check ) ) {
			return true;
		}

		//noinspection SimplifiableIfStatement
		if ( DynamicTypeDescriptorImpl.class.isInstance( check ) ) {
			return ( (DynamicTypeDescriptorImpl) check ).isAssignableTo( this );
		}

		return false;
	}

	public boolean isAssignableTo(DynamicTypeDescriptorImpl check) {
		if ( check.equals( superType ) ) {
			return true;
		}

		if ( superType.isAssignableTo( check ) ) {
			return true;
		}

		return false;
	}
}
