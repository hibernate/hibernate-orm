/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.xml.internal.attr;

import java.util.List;

import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyDiscriminatorValueMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingDiscriminatorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AnyAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValueAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValuesAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyKeTypeAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsJpaAnnotation;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.JoinColumn;

import static org.hibernate.boot.models.HibernateAnnotations.ANY_DISCRIMINATOR_VALUE;
import static org.hibernate.boot.models.JpaAnnotations.JOIN_COLUMN;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.*;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class AnyMappingAttributeProcessing {

	public static MutableMemberDetails processAnyMappingAttribute(
			JaxbAnyMappingImpl jaxbHbmAnyMapping,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbHbmAnyMapping.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbHbmAnyMapping.getName(),
				accessType,
				declarer
		);

		final AnyAnnotation anyAnn = (AnyAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.ANY,
				xmlDocumentContext.getModelBuildingContext()
		);

		applyAccess( accessType, memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbHbmAnyMapping, memberDetails, xmlDocumentContext );
		applyFetching( jaxbHbmAnyMapping, memberDetails, anyAnn, xmlDocumentContext );
		applyOptionality( jaxbHbmAnyMapping, anyAnn, xmlDocumentContext );
		applyOptimisticLock( jaxbHbmAnyMapping, memberDetails, xmlDocumentContext );

		applyDiscriminator( memberDetails, jaxbHbmAnyMapping, xmlDocumentContext );
		applyKey( memberDetails, jaxbHbmAnyMapping, xmlDocumentContext );
		XmlAnnotationHelper.applyCascading( jaxbHbmAnyMapping.getCascade(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}

	private static void applyDiscriminator(
			MutableMemberDetails memberDetails,
			JaxbAnyMappingImpl jaxbHbmAnyMapping,
			XmlDocumentContext xmlDocumentContext) {
		final JaxbAnyMappingDiscriminatorImpl jaxbDiscriminator = jaxbHbmAnyMapping.getDiscriminator();
		final AnyDiscriminatorAnnotation anyDiscriminatorAnn = (AnyDiscriminatorAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.ANY_DISCRIMINATOR,
				xmlDocumentContext.getModelBuildingContext()
		);

		if ( jaxbDiscriminator == null ) {
			return;
		}

		final DiscriminatorType discriminatorType = jaxbDiscriminator.getType();
		if ( discriminatorType != null ) {
			anyDiscriminatorAnn.value( discriminatorType );
		}

		final JaxbColumnImpl jaxbColumn = jaxbDiscriminator.getColumn();
		final ColumnJpaAnnotation columnAnn = (ColumnJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.COLUMN,
				xmlDocumentContext.getModelBuildingContext()
		);
		if ( jaxbColumn != null ) {
			columnAnn.apply( jaxbColumn, xmlDocumentContext );
		}

		final List<JaxbAnyDiscriminatorValueMappingImpl> jaxbValueMappings = jaxbDiscriminator.getValueMappings();
		if ( CollectionHelper.isNotEmpty( jaxbValueMappings ) ) {
			final AnyDiscriminatorValuesAnnotation discriminatorValuesUsage = (AnyDiscriminatorValuesAnnotation) memberDetails.replaceAnnotationUsage(
					ANY_DISCRIMINATOR_VALUE,
					HibernateAnnotations.ANY_DISCRIMINATOR_VALUES,
					xmlDocumentContext.getModelBuildingContext()
			);
			discriminatorValuesUsage.value( collectDiscriminatorValues(
					jaxbValueMappings,
					xmlDocumentContext
			) );
		}
	}

	private static AnyDiscriminatorValue[] collectDiscriminatorValues(
			List<JaxbAnyDiscriminatorValueMappingImpl> jaxbValueMappings,
			XmlDocumentContext xmlDocumentContext) {
		final AnyDiscriminatorValue[] values = new AnyDiscriminatorValue[jaxbValueMappings.size()];
		for ( int i = 0; i < jaxbValueMappings.size(); i++ ) {
			final AnyDiscriminatorValueAnnotation valueAnn = ANY_DISCRIMINATOR_VALUE.createUsage( xmlDocumentContext.getModelBuildingContext() );
			values[i] = valueAnn;

			final JaxbAnyDiscriminatorValueMappingImpl jaxbValue = jaxbValueMappings.get( i );

			valueAnn.discriminator( jaxbValue.getDiscriminatorValue() );

			final String name = StringHelper.qualifyConditionally(
					xmlDocumentContext.getXmlDocument().getDefaults().getPackage(),
					jaxbValue.getCorrespondingEntityName()
			);
			final ClassDetails entityClassDetails = xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry().resolveClassDetails( name );
			valueAnn.entity( entityClassDetails.toJavaClass() );
		}
		return values;
	}

	private static void applyKey(
			MutableMemberDetails memberDetails,
			JaxbAnyMappingImpl jaxbHbmAnyMapping,
			XmlDocumentContext xmlDocumentContext) {
		final JaxbAnyMappingKeyImpl jaxbKey = jaxbHbmAnyMapping.getKey();
		if ( StringHelper.isNotEmpty( jaxbKey.getType() ) ) {
			final AnyKeTypeAnnotation keyTypeUsage = (AnyKeTypeAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.ANY_KEY_TYPE,
					xmlDocumentContext.getModelBuildingContext()
			);
			keyTypeUsage.value( jaxbKey.getType() );
		}

		if ( jaxbKey.getColumns().isEmpty() ) {
			memberDetails.applyAnnotationUsage( JpaAnnotations.JOIN_COLUMN, xmlDocumentContext.getModelBuildingContext() );
		}
		else {
			final JoinColumnsJpaAnnotation joinColumnsUsage = (JoinColumnsJpaAnnotation) memberDetails.replaceAnnotationUsage(
					JOIN_COLUMN,
					JpaAnnotations.JOIN_COLUMNS,
					xmlDocumentContext.getModelBuildingContext()
			);

			final JoinColumn[] joinColumns = new JoinColumn[jaxbKey.getColumns().size()];
			joinColumnsUsage.value( joinColumns );

			for ( int i = 0; i < jaxbKey.getColumns().size(); i++ ) {
				final JoinColumnJpaAnnotation joinColumn = JOIN_COLUMN.createUsage( xmlDocumentContext.getModelBuildingContext() );
				joinColumns[i] = joinColumn;

				final JaxbColumnImpl jaxbJoinColumn = jaxbKey.getColumns().get( i );
				joinColumn.apply( jaxbJoinColumn, xmlDocumentContext );
			}
		}
	}

}
