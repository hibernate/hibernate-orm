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
package org.hibernate.metamodel.source.internal.annotations.util;

import java.util.regex.Pattern;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.AttributeConversionInfo;
import org.hibernate.metamodel.source.internal.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.OverrideAndConverterCollector;
import org.hibernate.metamodel.source.internal.annotations.attribute.PersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.spi.AttributePath;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

/**
 * Helper for working with AttributeConverters, AttributeOverrides and AssociationOverrides
 *
 * @author Steve Ebersole
 */
public class ConverterAndOverridesHelper {
	private static final Logger log = Logger.getLogger( ConverterAndOverridesHelper.class );

	/**
	 * Singleton access
	 */
	public static final ConverterAndOverridesHelper INSTANCE = new ConverterAndOverridesHelper();

	private ConverterAndOverridesHelper() {
	}


	/**
	 * Process Convert/Converts annotations found on the member for an attribute.
	 *
	 * @param path The path to the attribute
	 * @param nature The attribute nature
	 * @param backingMember The descriptor for the Java member backing the attribute
	 * @param container The attribute container (where converters are collected)
	 * @param context The binding context
	 */
	public void processConverters(
			AttributePath path,
			PersistentAttribute.Nature nature,
			MemberDescriptor backingMember,
			OverrideAndConverterCollector container,
			EntityBindingContext context) {
		{
			final AnnotationInstance convertAnnotation = backingMember.getAnnotations().get( JPADotNames.CONVERT );
			if ( convertAnnotation != null ) {
				processConverter( convertAnnotation, path, nature, container, backingMember, context );
			}
		}

		{
			final AnnotationInstance convertsAnnotation = backingMember.getAnnotations().get( JPADotNames.CONVERTS );
			if ( convertsAnnotation != null ) {
				final AnnotationInstance[] convertAnnotations = JandexHelper.extractAnnotationsValue(
						convertsAnnotation,
						"value"
				);
				for ( AnnotationInstance convertAnnotation : convertAnnotations ) {
					processConverter( convertAnnotation, path, nature, container, backingMember, context );
				}
			}
		}
	}

	private void processConverter(
			AnnotationInstance convertAnnotation,
			AttributePath path,
			PersistentAttribute.Nature nature,
			OverrideAndConverterCollector container,
			MemberDescriptor backingMember,
			EntityBindingContext context) {
		final AttributeConversionInfo conversionInfo;

		final AnnotationValue isDisabledValue = convertAnnotation.value( "disableConversion" );
		final boolean isDisabled = isDisabledValue != null && isDisabledValue.asBoolean();
		if ( isDisabled ) {
			// no need to even touch the class
			conversionInfo = new AttributeConversionInfo( false, null );
		}
		else {
			final AnnotationValue converterClassNameValue = convertAnnotation.value( "converter" );
			// if `converterValue` is null, not sure this annotation really serves any purpose...
			String converterClassName = converterClassNameValue == null
					? null
					: converterClassNameValue.asString();
			if ( "void".equals( converterClassName ) ) {
				converterClassName = null;
			}

			if ( converterClassName == null ) {
				// again, at this point not really sure what purpose this annotation serves

				return;
			}
			conversionInfo = new AttributeConversionInfo(
					true,
					(ClassDescriptor) context.getJavaTypeDescriptorRepository().getType(
							DotName.createSimple( converterClassName )
					)
			);
		}

		final AnnotationValue specifiedNameValue = convertAnnotation.value( "attributeName" );
		final String specifiedName = specifiedNameValue == null
				? null
				: StringHelper.nullIfEmpty( specifiedNameValue.asString() );

		if ( specifiedName == null ) {
			// No attribute name was specified.  According to the spec, this is
			// ok (in fact expected) if the attribute is basic in nature
			if ( nature != PersistentAttribute.Nature.BASIC
					&& nature != PersistentAttribute.Nature.ELEMENT_COLLECTION_BASIC ) {
				// now here we CAN throw the exception
				throw context.makeMappingException(
						"Found @Convert with no `attributeName` specified on a non-basic attribute : " + backingMember.toString()
				);
			}
			container.registerConverter( path, conversionInfo );
		}
		else {
			// An attribute name was specified.  Technically we should make
			// sure that the attribute is NOT basic in nature.  Collections are
			// a bit difficult here since the element/map-key could be basic while
			// the converter refers to the other which is non-basic
			if ( nature == PersistentAttribute.Nature.BASIC ) {
				log.debugf(
						"Found @Convert with `attributeName` specified (%s) on a non-basic attribute : %s",
						specifiedName,
						backingMember.toString()
				);
			}
			container.registerConverter( buildAttributePath( path, specifiedName ), conversionInfo );
		}
	}

	private AttributePath buildAttributePath(AttributePath base, String specifiedName) {
		assert base != null;
		assert specifiedName != null;

		// these names can contain paths themselves...
		AttributePath pathSoFar = base;
		for ( String pathPart : specifiedName.split( Pattern.quote( "." ) ) ) {
			pathSoFar = pathSoFar.append( pathPart );
		}

		return pathSoFar;
	}


	/**
	 * Process AttributeConverter (plus plural) annotations found on the member for an attribute.
	 *
	 * @param path The path to the attribute
	 * @param backingMember The descriptor for the Java member backing the attribute
	 * @param container The attribute container (where overrides are collected)
	 * @param context The binding context
	 */
	public void processAttributeOverrides(
			AttributePath path,
			MemberDescriptor backingMember,
			OverrideAndConverterCollector container,
			EntityBindingContext context) {
		processAttributeOverrides(
				path,
				backingMember.getAnnotations().get( JPADotNames.ATTRIBUTE_OVERRIDE ),
				backingMember.getAnnotations().get( JPADotNames.ATTRIBUTE_OVERRIDES ),
				container,
				context
		);
	}

	private void processAttributeOverrides(
			AttributePath path,
			AnnotationInstance singular,
			AnnotationInstance plural,
			OverrideAndConverterCollector container,
			EntityBindingContext context) {
		if ( singular != null ) {
			processAttributeOverride( singular, path, container, context );
		}

		if ( plural != null ) {
			final AnnotationInstance[] overrideAnnotations = JandexHelper.extractAnnotationsValue(
					plural,
					"value"
			);
			for ( AnnotationInstance overrideAnnotation : overrideAnnotations ) {
				processAttributeOverride( overrideAnnotation, path, container, context );
			}
		}

	}

	private void processAttributeOverride(
			AnnotationInstance overrideAnnotation,
			AttributePath path,
			OverrideAndConverterCollector container,
			EntityBindingContext context) {
		// the name is required...
		final String specifiedPath = overrideAnnotation.value( "name" ).asString();
		container.registerAttributeOverride(
				buildAttributePath( path, specifiedPath ),
				new AttributeOverride( path.getFullPath(), overrideAnnotation, context )
		);
	}

	public void processAssociationOverrides(
			AttributePath path,
			MemberDescriptor backingMember,
			OverrideAndConverterCollector container,
			EntityBindingContext context) {
		{
			final AnnotationInstance overrideAnnotation = backingMember.getAnnotations().get( JPADotNames.ASSOCIATION_OVERRIDE );
			if ( overrideAnnotation != null ) {
				processAssociationOverride( overrideAnnotation, path, container, context );
			}
		}

		{
			final AnnotationInstance overridesAnnotation = backingMember.getAnnotations().get( JPADotNames.ASSOCIATION_OVERRIDES );
			if ( overridesAnnotation != null ) {
				final AnnotationInstance[] overrideAnnotations = JandexHelper.extractAnnotationsValue(
						overridesAnnotation,
						"value"
				);
				for ( AnnotationInstance overrideAnnotation : overrideAnnotations ) {
					processAssociationOverride( overrideAnnotation, path, container, context );
				}
			}
		}
	}

	private void processAssociationOverride(
			AnnotationInstance overrideAnnotation,
			AttributePath path,
			OverrideAndConverterCollector container,
			EntityBindingContext context) {
		// the name is required...
		final String specifiedPath = overrideAnnotation.value( "name" ).asString();
		container.registerAssociationOverride(
				buildAttributePath( path, specifiedPath ),
				new AssociationOverride( path.getFullPath(), overrideAnnotation, context )
		);
	}

	/**
	 * Process AttributeConverter (plus plural) annotations found on the class.
	 *
	 * @param attributePath The path to the attribute
	 * @param typeMetadata The descriptor for the Java member backing the attribute
	 * @param container The attribute container (where overrides are collected)
	 * @param context The binding context
	 */
	public void processAttributeOverrides(
			AttributePath attributePath,
			ManagedTypeMetadata typeMetadata,
			OverrideAndConverterCollector container,
			EntityBindingContext context) {
		processAttributeOverrides(
				attributePath,
				typeMetadata.getJavaTypeDescriptor().findLocalTypeAnnotation( JPADotNames.ATTRIBUTE_OVERRIDE ),
				typeMetadata.getJavaTypeDescriptor().findLocalTypeAnnotation( JPADotNames.ATTRIBUTE_OVERRIDES ),
				container,
				context
		);
	}
}
