/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.Access;
import jakarta.persistence.Embedded;
import org.hibernate.MappingException;
import org.hibernate.annotations.TargetEmbeddable;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.models.spi.TypeDetails.Kind;
import org.hibernate.models.spi.TypeVariableScope;

/**
 * Retrieve all inferred data from an annotated element
 *
 * @author Emmanuel Bernard
 * @author Paolo Perrotta
 */
public class PropertyInferredData implements PropertyData {
	private final AccessType defaultAccess;

	private final ClassDetails declaringClass;
	private final TypeVariableScope ownerType;
	private final MemberDetails propertyMember;
	private final MetadataBuildingContext buildingContext;

	/**
	 * Take the annotated element for lazy process
	 */
	public PropertyInferredData(
			ClassDetails declaringClass,
			TypeVariableScope ownerType,
			MemberDetails propertyMember,
			String propertyAccessor,
			MetadataBuildingContext buildingContext) {
		this.declaringClass = declaringClass;
		this.ownerType = ownerType;
		this.propertyMember = propertyMember;
		this.defaultAccess = AccessType.getAccessStrategy( propertyAccessor );
		this.buildingContext = buildingContext;
	}

	@Override
	public String toString() {
		return String.format( "PropertyInferredData{property=%s, declaringClass=%s}", propertyMember, declaringClass );
	}

	@Override
	public AccessType getDefaultAccess() throws MappingException {
		AccessType accessType = defaultAccess;

		AccessType jpaAccessType = AccessType.DEFAULT;

		Access access = propertyMember.getDirectAnnotationUsage( Access.class );
		if ( access != null ) {
			jpaAccessType = AccessType.getAccessStrategy( access.value() );
		}

		if ( jpaAccessType != AccessType.DEFAULT ) {
			accessType = jpaAccessType;
		}
		return accessType;
	}

	@Override
	public String getPropertyName() throws MappingException {
		return propertyMember.resolveAttributeName();
	}

	@Override
	public TypeDetails getPropertyType() throws MappingException {
		final org.hibernate.boot.internal.Target targetAnnotation = propertyMember.getDirectAnnotationUsage( org.hibernate.boot.internal.Target.class );
		final ModelsContext sourceModelContext = buildingContext.getBootstrapContext().getModelsContext();
		if ( targetAnnotation != null ) {
			final String targetName = targetAnnotation.value();
			final ClassDetails classDetails = ModelsHelper.resolveClassDetails(
					targetName,
					sourceModelContext.getClassDetailsRegistry(),
					() -> new DynamicClassDetails( targetName, sourceModelContext )
			);
			return new ClassTypeDetailsImpl( classDetails, Kind.CLASS );
		}

		final TargetEmbeddable targetEmbeddable = getTargetEmbeddableAnnotation(propertyMember);
		if ( targetEmbeddable != null ) {
			return resolveTargetEmbeddableAnnotation( targetEmbeddable, sourceModelContext );
		}

		return propertyMember.resolveRelativeType( ownerType );
	}

	private TargetEmbeddable getTargetEmbeddableAnnotation(MemberDetails memberDetails) {
		TargetEmbeddable targetEmbeddable = memberDetails.getDirectAnnotationUsage( TargetEmbeddable.class );
		if ( targetEmbeddable != null ) {
			if (!memberDetails.hasDirectAnnotationUsage(Embedded.class) ) {
				// If set on an attribute, it should be an @Embedded
				throw new MappingException( "The @TargetEmbeddable annotation can only be set on an element marked with @Embedded" );
			}
		}
		else {
			// Set on an interface
			targetEmbeddable = memberDetails.getAssociatedType().determineRawClass().getDirectAnnotationUsage( TargetEmbeddable.class );
		}
		return targetEmbeddable;
	}

	private static ClassTypeDetailsImpl resolveTargetEmbeddableAnnotation(
			TargetEmbeddable targetEmbeddable,
			ModelsContext sourceModelContext) {
		final ClassDetailsRegistry classDetailsRegistry = sourceModelContext.getClassDetailsRegistry();
		final ClassDetails targetClassDetails = classDetailsRegistry.resolveClassDetails( targetEmbeddable.value().getName() );
		return new ClassTypeDetailsImpl( targetClassDetails, Kind.CLASS );
	}

	@Override
	public TypeDetails getClassOrElementType() throws MappingException {
		final ModelsContext modelsContext = buildingContext.getBootstrapContext().getModelsContext();
		final org.hibernate.boot.internal.Target annotationUsage = propertyMember.getDirectAnnotationUsage( org.hibernate.boot.internal.Target.class );
		if ( annotationUsage != null ) {
			final String targetName = annotationUsage.value();
			final ClassDetails classDetails = ModelsHelper.resolveClassDetails(
					targetName,
					modelsContext.getClassDetailsRegistry(),
					() -> new DynamicClassDetails( targetName, modelsContext )
			);
			return new ClassTypeDetailsImpl( classDetails, Kind.CLASS );
		}

		final TargetEmbeddable targetEmbeddable = getTargetEmbeddableAnnotation(propertyMember);
		if ( targetEmbeddable != null ) {
			return resolveTargetEmbeddableAnnotation( targetEmbeddable, modelsContext );
		}

		return propertyMember.resolveRelativeAssociatedType( ownerType );
	}

	@Override
	public ClassDetails getClassOrPluralElement() throws MappingException {
		final org.hibernate.boot.internal.Target xmlTarget = propertyMember.getDirectAnnotationUsage( org.hibernate.boot.internal.Target.class );
		if ( xmlTarget != null ) {
			return buildingContext.getMetadataCollector().getClassDetailsRegistry().getClassDetails( xmlTarget.value() );
		}

		final TargetEmbeddable targetEmbeddable = getTargetEmbeddableAnnotation(propertyMember);
		if ( targetEmbeddable != null ) {
			final String targetName = targetEmbeddable.value().getName();
			return buildingContext.getMetadataCollector().getClassDetailsRegistry().getClassDetails( targetName );
		}

		if ( propertyMember.isPlural() ) {
			return propertyMember.getElementType().determineRawClass();
		}

		return propertyMember.getAssociatedType().determineRawClass();
	}

	@Override
	public String getClassOrElementName() throws MappingException {
		return getClassOrElementType().getName();
	}

	@Override
	public String getTypeName() throws MappingException {
		return getPropertyType().getName();
	}

	@Override
	public MemberDetails getAttributeMember() {
		return propertyMember;
	}

	@Override
	public ClassDetails getDeclaringClass() {
		return declaringClass;
	}


}
