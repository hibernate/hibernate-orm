/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import java.util.Locale;

import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.JavaTypeAnnotation;
import org.hibernate.boot.models.annotations.internal.TemporalJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TypeAnnotation;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
public class UserTypeCasesStandard extends AbstractUserTypeCases {
	public static final UserTypeCasesStandard STANDARD_USER_TYPE_CASES = new UserTypeCasesStandard();

	public static void applyJavaTypeAnnotationStatic(
			MutableMemberDetails memberDetails,
			Class<? extends BasicJavaType<?>> descriptor,
			XmlDocumentContext xmlDocumentContext) {
		final JavaTypeAnnotation javaTypeAnnotation = (JavaTypeAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.JAVA_TYPE,
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
		final Temporal directUsage = memberDetails.getDirectAnnotationUsage( Temporal.class );
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

		final TemporalJpaAnnotation temporalAnnotation = (TemporalJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.TEMPORAL,
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
		final TypeAnnotation typeAnn = (TypeAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.TYPE,
				xmlDocumentContext.getModelBuildingContext()
		);
		typeAnn.value( userTypeImpl.toJavaClass() );
		typeAnn.parameters( XmlAnnotationHelper.collectParameters( jaxbType.getParameters(), xmlDocumentContext ) );
	}
}
