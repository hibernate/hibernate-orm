/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.Collection;

import org.hibernate.MappingException;
import org.hibernate.annotations.Target;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.models.spi.TypeVariableScope;

import jakarta.persistence.Access;

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

		AnnotationUsage<Access> access = propertyMember.getAnnotationUsage( Access.class );
		if ( access != null ) {
			jpaAccessType = AccessType.getAccessStrategy( access.getEnum( "value" ) );
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
		final AnnotationUsage<org.hibernate.boot.internal.Target> targetAnnotation = propertyMember.getAnnotationUsage( org.hibernate.boot.internal.Target.class );
		if ( targetAnnotation != null ) {
			return new ClassTypeDetailsImpl( targetAnnotation.getClassDetails( "value" ), TypeDetails.Kind.CLASS );
		}

		final AnnotationUsage<Target> legacyTargetAnnotation = propertyMember.getAnnotationUsage( Target.class );
		if ( legacyTargetAnnotation != null ) {
			return new ClassTypeDetailsImpl( legacyTargetAnnotation.getClassDetails( "value" ), TypeDetails.Kind.CLASS );
		}

		return propertyMember.resolveRelativeType( ownerType );
	}

	@Override
	public TypeDetails getClassOrElementType() throws MappingException {
		final AnnotationUsage<org.hibernate.boot.internal.Target> annotationUsage = propertyMember.getAnnotationUsage( org.hibernate.boot.internal.Target.class );
		if ( annotationUsage != null ) {
			return new ClassTypeDetailsImpl( annotationUsage.getClassDetails( "value" ), TypeDetails.Kind.CLASS );
		}

		final AnnotationUsage<Target> legacyAnnotationUsage = propertyMember.getAnnotationUsage( Target.class );
		if ( legacyAnnotationUsage != null ) {
			return new ClassTypeDetailsImpl( legacyAnnotationUsage.getClassDetails( "value" ), TypeDetails.Kind.CLASS );
		}

		return propertyMember.getAssociatedType().determineRelativeType( ownerType );
	}

	@Override
	public ClassDetails getClassOrPluralElement() throws MappingException {
		final AnnotationUsage<Target> targetAnnotationUsage = propertyMember.getAnnotationUsage( Target.class );
		if ( targetAnnotationUsage != null ) {
			return targetAnnotationUsage.getClassDetails( "value" );
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
