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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.metamodel.reflite.spi.DynamicAttributeDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.ParameterizedType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

/**
 * @author Steve Ebersole
 */
public class DynamicAttributeDescriptorImpl implements DynamicAttributeDescriptor, AnnotationTarget {
	private final DynamicTypeDescriptorImpl declaringType;
	private final String name;

	private ParameterizedType type;

	private DynamicAnnotationBuilder annotationBuilder;
	private Map<DotName, AnnotationInstance> annotationMap;

	public DynamicAttributeDescriptorImpl(
			DynamicTypeDescriptorImpl declaringType,
			String name,
			ParameterizedType type) {
		this.declaringType = declaringType;
		this.name = name;
		this.type = type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ParameterizedType getType() {
		return type;
	}

	public void setType(ParameterizedType type) {
		if ( this.type != null ) {
			throw new IllegalStateException( "Dynamic attribute type already set" );
		}
		this.type = type;
	}

	@Override
	public int getModifiers() {
		return Modifier.PUBLIC;
	}

	@Override
	public JavaTypeDescriptor getDeclaringType() {
		return declaringType;
	}

	public DynamicAnnotationBuilder getAnnotationBuilder() {
		if ( annotationBuilder == null ) {
			annotationBuilder = new DynamicAttributeAnnotationBuilder( this );
		}
		return annotationBuilder;
	}

	@Override
	public Map<DotName, AnnotationInstance> getAnnotations() {
		return annotationMap;
	}

	@Override
	public String toLoggableForm() {
		return declaringType.getName().toString() + '#' + name;
	}

	@Override
	public String toSignatureForm() {
		return type.getErasedType().getName().toString() + ' ' + name;
	}

	@Override
	public String toString() {
		return "FieldDescriptorImpl{" + declaringType.getName().toString() + '#' + name + '}';
	}

	private static class DynamicAttributeAnnotationBuilder implements DynamicAnnotationBuilder {
		private final DynamicAttributeDescriptorImpl attribute;

		private DynamicAttributeAnnotationBuilder(DynamicAttributeDescriptorImpl attribute) {
			this.attribute = attribute;
		}

		@Override
		public AnnotationInstance makeAnnotation(DotName annotationTypeName, AnnotationValue... values) {
			final AnnotationInstance annotationInstance = AnnotationInstance.create( annotationTypeName, attribute, values );

			attribute.addAnnotation( annotationInstance );

			return annotationInstance;
		}

		@Override
		public AnnotationInstance makeAnnotation(DotName annotationTypeName, List<AnnotationValue> values) {
			final AnnotationInstance annotationInstance = AnnotationInstance.create( annotationTypeName, attribute, values );

			attribute.addAnnotation( annotationInstance );

			return annotationInstance;
		}
	}

	private void addAnnotation(AnnotationInstance annotationInstance) {
		if ( annotationMap == null ) {
			annotationMap = new HashMap<DotName, AnnotationInstance>();
		}

		final AnnotationInstance old = annotationMap.get( annotationInstance.name() );
		if ( old != null ) {
			throw new IllegalAnnotationException(
					String.format(
							Locale.ENGLISH,
							"%s already contained a type annotation instance for %s : %s",
							toLoggableForm(),
							annotationInstance.name().toString(),
							old
					)
			);
		}
	}
}
