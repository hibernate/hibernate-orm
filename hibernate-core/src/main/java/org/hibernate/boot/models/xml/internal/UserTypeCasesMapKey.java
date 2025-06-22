/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import java.util.Locale;

import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.MapKeyJavaTypeAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyTemporalJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyTypeAnnotation;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
public class UserTypeCasesMapKey extends AbstractUserTypeCases {
	public static final UserTypeCasesMapKey MAP_KEY_USER_TYPE_CASES = new UserTypeCasesMapKey();

	public static void applyJavaTypeAnnotationStatic(
			MutableMemberDetails memberDetails,
			Class<? extends BasicJavaType<?>> descriptor,
			XmlDocumentContext xmlDocumentContext) {
		final MapKeyJavaTypeAnnotation javaTypeAnnotation = (MapKeyJavaTypeAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.MAP_KEY_JAVA_TYPE,
				xmlDocumentContext.getModelBuildingContext()
		);
		javaTypeAnnotation.value( descriptor );
	}

	@Override
	protected void applyJavaTypeAnnotation(
			MutableMemberDetails memberDetails,
			Class<? extends BasicJavaType<?>> descriptor,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotationStatic( memberDetails, descriptor, xmlDocumentContext );
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void applyTemporalPrecision(
			MutableMemberDetails memberDetails,
			TemporalType temporalType,
			XmlDocumentContext xmlDocumentContext) {
		final MapKeyTemporal directUsage = memberDetails.getDirectAnnotationUsage( MapKeyTemporal.class );
		if ( directUsage != null ) {
			// make sure they match
			if ( directUsage.value() != temporalType ) {
				throw new org.hibernate.MappingException( String.format(
						Locale.ROOT,
						"Mismatch in expected TemporalType on %s; found %s and %s",
						memberDetails,
						directUsage.value(),
						temporalType
				) );
			}
			return;
		}

		final MapKeyTemporalJpaAnnotation temporalAnnotation = (MapKeyTemporalJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.MAP_KEY_TEMPORAL,
				xmlDocumentContext.getModelBuildingContext()
		);
		temporalAnnotation.value( temporalType );
	}

	@Override
	public void handleGeneral(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final ClassDetails userTypeImpl = XmlAnnotationHelper.resolveJavaType( jaxbType.getValue(), xmlDocumentContext );
		assert userTypeImpl.isImplementor( UserType.class );
		final MapKeyTypeAnnotation typeAnn = (MapKeyTypeAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.MAP_KEY_TYPE,
				xmlDocumentContext.getModelBuildingContext()
		);
		typeAnn.value( userTypeImpl.toJavaClass() );
		typeAnn.parameters( XmlAnnotationHelper.collectParameters( jaxbType.getParameters(), xmlDocumentContext ) );
	}
}
