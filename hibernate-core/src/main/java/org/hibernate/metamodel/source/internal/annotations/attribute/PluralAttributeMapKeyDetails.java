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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolverComposition;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.EnumeratedTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.HibernateTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.TemporalTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;


/**
 * PluralAttributeIndexDetails implementation for describing the key of a Map
 *
 * @author Steve Ebersole
 */
public class PluralAttributeMapKeyDetails implements PluralAttributeIndexDetails {
	private final PluralAttribute pluralAttribute;

	private final AnnotationInstance mapKeyAnnotation;
	private final AnnotationInstance mapKeyClassAnnotation;

	private final AnnotationInstance mapKeyColumnAnnotation;
	private final List<AnnotationInstance> mapKeyJoinColumnAnnotations;

	private final JavaTypeDescriptor resolvedMapKeyType;
	private final AttributeTypeResolver typeResolver;

	public PluralAttributeMapKeyDetails(
			PluralAttribute pluralAttribute,
			MemberDescriptor backingMember,
			JavaTypeDescriptor resolvedMapKeyType) {
		this.pluralAttribute = pluralAttribute;

		this.mapKeyAnnotation = backingMember.getAnnotations().get( JPADotNames.MAP_KEY );
		this.mapKeyClassAnnotation = backingMember.getAnnotations().get( JPADotNames.MAP_KEY_CLASS );

		if ( mapKeyAnnotation != null && mapKeyClassAnnotation != null ) {
			// this is an error according to the spec...
			throw pluralAttribute.getContext().makeMappingException(
					"Map attribute defined both @MapKey and @MapKeyClass; only one should be used : " +
							backingMember.toLoggableForm()
			);
		}

		this.mapKeyColumnAnnotation = backingMember.getAnnotations().get( JPADotNames.MAP_KEY_COLUMN );
		this.mapKeyJoinColumnAnnotations = collectMapKeyJoinColumnAnnotations( backingMember );

		this.resolvedMapKeyType = determineMapKeyJavaType( backingMember, resolvedMapKeyType, mapKeyClassAnnotation );

		this.typeResolver = new AttributeTypeResolverComposition(
				resolvedMapKeyType,
				pluralAttribute.getContext(),
				HibernateTypeResolver.createCollectionIndexTypeResolver( pluralAttribute, resolvedMapKeyType ),
				EnumeratedTypeResolver.createCollectionIndexTypeResolver( pluralAttribute, resolvedMapKeyType ),
				TemporalTypeResolver.createCollectionIndexTypeResolver( pluralAttribute, resolvedMapKeyType )
		);
	}

	private JavaTypeDescriptor determineMapKeyJavaType(
			MemberDescriptor backingMember,
			JavaTypeDescriptor resolvedMapKeyType,
			AnnotationInstance mapKeyClassAnnotation) {
		if ( mapKeyClassAnnotation != null ) {
			final AnnotationValue value = mapKeyClassAnnotation.value();
			assert value != null : "Unexpected null from @MapKeyClass.value : " + backingMember.toLoggableForm();
			return pluralAttribute.getContext().getJavaTypeDescriptorRepository().getType( value.asClass().name() );
		}

		return resolvedMapKeyType;
	}

	private List<AnnotationInstance> collectMapKeyJoinColumnAnnotations(MemberDescriptor backingMember) {
		final AnnotationInstance singular = backingMember.getAnnotations().get( JPADotNames.MAP_KEY_JOIN_COLUMN );
		final AnnotationInstance plural = backingMember.getAnnotations().get( JPADotNames.MAP_KEY_JOIN_COLUMNS );

		if ( singular != null && plural != null ) {
			throw pluralAttribute.getContext().makeMappingException(
					"Attribute [" + backingMember.toLoggableForm() +
							"] declared both @MapKeyJoinColumn and " +
							"@MapKeyJoinColumns; should only use one or the other"
			);
		}

		if ( singular == null && plural == null ) {
			return null;
		}

		if ( singular != null ) {
			return Collections.singletonList( singular );
		}

		final AnnotationInstance[] annotations = JandexHelper.extractAnnotationsValue(
				plural,
				"value"
		);
		if ( annotations == null || annotations.length == 0 ) {
			return null;
		}

		return Arrays.asList( annotations );
	}

	/**
	 * Get the {@link javax.persistence.MapKey} annotation descriptor, if one.
	 *
	 * @return The @MapKey annotation, or {@code null}
	 */
	public AnnotationInstance getMapKeyAnnotation() {
		return mapKeyAnnotation;
	}

	/**
	 * Get the {@link javax.persistence.MapKeyClass} annotation descriptor, if one.
	 *
	 * @return The @MapKeyClass annotation, or {@code null}
	 */
	public AnnotationInstance getMapKeyClassAnnotation() {
		return mapKeyClassAnnotation;
	}

	public AnnotationInstance getMapKeyColumnAnnotation() {
		return mapKeyColumnAnnotation;
	}

	public List<AnnotationInstance> getMapKeyJoinColumnAnnotations() {
		return mapKeyJoinColumnAnnotations;
	}

	@Override
	public JavaTypeDescriptor getJavaType() {
		return resolvedMapKeyType;
	}

	@Override
	public AttributeTypeResolver getTypeResolver() {
		return typeResolver;
	}
}
