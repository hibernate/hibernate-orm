/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAssociationOverrideImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributeOverrideImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBaseAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbForeignKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTransientImpl;
import org.hibernate.boot.models.xml.internal.attr.AnyMappingAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.BasicAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.ElementCollectionAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.EmbeddedAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.ManyToManyAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.ManyToOneAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.OneToManyAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.OneToOneAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.PluralAnyMappingAttributeProcessing;
import org.hibernate.boot.models.xml.internal.db.ColumnProcessing;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.internal.db.TableProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;

/**
 * Helper for handling persistent attributes defined in mapping XML in metadata-complete mode
 *
 * @author Steve Ebersole
 */
public class AttributeProcessor {
	public static void processNaturalId(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		processNaturalId( jaxbNaturalId, mutableClassDetails, classAccessType, null, xmlDocumentContext );
	}

	public static void processNaturalId(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			MemberAdjuster memberAdjuster,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNaturalId == null ) {
			return;
		}

		XmlAnnotationHelper.applyNaturalIdCache( jaxbNaturalId, mutableClassDetails, xmlDocumentContext );

		processBaseAttributes( jaxbNaturalId, mutableClassDetails, classAccessType, memberAdjuster, xmlDocumentContext );
	}

	public static void processBaseAttributes(
			JaxbBaseAttributesContainer attributesContainer,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			MemberAdjuster memberAdjuster,
			XmlDocumentContext xmlDocumentContext) {
		for ( int i = 0; i < attributesContainer.getBasicAttributes().size(); i++ ) {
			final JaxbBasicImpl jaxbBasic = attributesContainer.getBasicAttributes().get( i );
			final MutableMemberDetails memberDetails = BasicAttributeProcessing.processBasicAttribute(
					jaxbBasic,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbBasic, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getEmbeddedAttributes().size(); i++ ) {
			final JaxbEmbeddedImpl jaxbEmbedded = attributesContainer.getEmbeddedAttributes().get( i );
			final MutableMemberDetails memberDetails = EmbeddedAttributeProcessing.processEmbeddedAttribute(
					jaxbEmbedded,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbEmbedded, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getManyToOneAttributes().size(); i++ ) {
			final JaxbManyToOneImpl jaxbManyToOne = attributesContainer.getManyToOneAttributes().get( i );
			final MutableMemberDetails memberDetails = ManyToOneAttributeProcessing.processManyToOneAttribute(
					jaxbManyToOne,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbManyToOne, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getAnyMappingAttributes().size(); i++ ) {
			final JaxbAnyMappingImpl jaxbAnyMapping = attributesContainer.getAnyMappingAttributes().get( i );
			final MutableMemberDetails memberDetails = AnyMappingAttributeProcessing.processAnyMappingAttribute(
					jaxbAnyMapping,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbAnyMapping, xmlDocumentContext );
			}
		}
	}

	@FunctionalInterface
	public interface MemberAdjuster {
		<M extends MutableMemberDetails> void adjust(M member, JaxbPersistentAttribute jaxbPersistentAttribute, XmlDocumentContext xmlDocumentContext);
	}

	public static void processAttributes(
			JaxbAttributesContainer attributesContainer,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		processAttributes( attributesContainer, mutableClassDetails, classAccessType, null, xmlDocumentContext );
	}

	public static void processAttributes(
			JaxbAttributesContainer attributesContainer,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			MemberAdjuster memberAdjuster,
			XmlDocumentContext xmlDocumentContext) {
		processBaseAttributes( attributesContainer, mutableClassDetails, classAccessType, memberAdjuster, xmlDocumentContext );

		for ( int i = 0; i < attributesContainer.getOneToOneAttributes().size(); i++ ) {
			final JaxbOneToOneImpl jaxbOneToOne = attributesContainer.getOneToOneAttributes().get( i );
			final MutableMemberDetails memberDetails = OneToOneAttributeProcessing.processOneToOneAttribute(
					jaxbOneToOne,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbOneToOne, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getElementCollectionAttributes().size(); i++ ) {
			final JaxbElementCollectionImpl jaxbElementCollection = attributesContainer.getElementCollectionAttributes().get( i );
			final MutableMemberDetails memberDetails = ElementCollectionAttributeProcessing.processElementCollectionAttribute(
					jaxbElementCollection,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbElementCollection, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getOneToManyAttributes().size(); i++ ) {
			final JaxbOneToManyImpl jaxbOneToMany = attributesContainer.getOneToManyAttributes().get( i );
			final MutableMemberDetails memberDetails = OneToManyAttributeProcessing.processOneToManyAttribute(
					jaxbOneToMany,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbOneToMany, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getManyToManyAttributes().size(); i++ ) {
			final JaxbManyToManyImpl jaxbManyToMany = attributesContainer.getManyToManyAttributes().get( i );
			final MutableMemberDetails memberDetails = ManyToManyAttributeProcessing.processManyToManyAttribute(
					jaxbManyToMany,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbManyToMany, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getPluralAnyMappingAttributes().size(); i++ ) {
			final JaxbPluralAnyMappingImpl jaxbPluralAnyMapping = attributesContainer.getPluralAnyMappingAttributes()
					.get( i );
			final MutableMemberDetails memberDetails = PluralAnyMappingAttributeProcessing.processPluralAnyMappingAttributes(
					jaxbPluralAnyMapping,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbPluralAnyMapping, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getTransients().size(); i++ ) {
			final JaxbTransientImpl jaxbTransient = attributesContainer.getTransients().get( i );
			CommonAttributeProcessing.applyTransient(
					jaxbTransient,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
		}
	}

	public static void processAttributeOverrides(
			List<JaxbAttributeOverrideImpl> attributeOverrides,
			MutableClassDetails mutableClassDetails,
			XmlDocumentContext xmlDocumentContext) {
		final List<MutableAnnotationUsage<AttributeOverride>> attributeOverrideList = new ArrayList<>( attributeOverrides.size() );
		for ( JaxbAttributeOverrideImpl attributeOverride : attributeOverrides ) {
			final MutableAnnotationUsage<AttributeOverride> attributeOverrideAnn = XmlProcessingHelper.makeNestedAnnotation(
					AttributeOverride.class,
					mutableClassDetails,
					xmlDocumentContext
			);
			attributeOverrideList.add( attributeOverrideAnn );

			final MutableAnnotationUsage<Column> attributeOverrideColumnAnn = XmlProcessingHelper.makeNestedAnnotation(
					Column.class,
					mutableClassDetails,
					xmlDocumentContext
			);
			ColumnProcessing.applyColumnDetails( attributeOverride.getColumn(), attributeOverrideColumnAnn, xmlDocumentContext );
			attributeOverrideAnn.setAttributeValue( "name", attributeOverride.getName() );
			attributeOverrideAnn.setAttributeValue( "column", attributeOverrideColumnAnn );
		}

		final MutableAnnotationUsage<AttributeOverrides> attributeOverridesAnn = XmlProcessingHelper.getOrMakeAnnotation(
				AttributeOverrides.class,
				mutableClassDetails,
				xmlDocumentContext
		);
		attributeOverridesAnn.setAttributeValue( "value", attributeOverrideList );
	}

	public static void processAssociationOverrides(
			List<JaxbAssociationOverrideImpl> associationOverrides,
			MutableClassDetails mutableClassDetails,
			XmlDocumentContext xmlDocumentContext) {
		final List<MutableAnnotationUsage<AssociationOverride>> associationOverrideList = new ArrayList<>( associationOverrides.size() );
		for ( JaxbAssociationOverrideImpl associationOverride : associationOverrides ) {
			final MutableAnnotationUsage<AssociationOverride> attributeOverrideAnn = XmlProcessingHelper.makeNestedAnnotation(
					AssociationOverride.class,
					mutableClassDetails,
					xmlDocumentContext
			);
			associationOverrideList.add( attributeOverrideAnn );

			attributeOverrideAnn.setAttributeValue( "name", associationOverride.getName() );
			attributeOverrideAnn.setAttributeValue(
					"joinColumns",
					JoinColumnProcessing.createJoinColumns(
							associationOverride.getJoinColumns(),
							mutableClassDetails,
							xmlDocumentContext
					)
			);
			final JaxbForeignKeyImpl foreignKeys = associationOverride.getForeignKeys();
			if ( foreignKeys != null ) {
				attributeOverrideAnn.setAttributeValue(
						"foreignKey",
						ForeignKeyProcessing.createNestedForeignKeyAnnotation(
								foreignKeys,
								mutableClassDetails,
								xmlDocumentContext
						)
				);
			}
			final JaxbJoinTableImpl joinTable = associationOverride.getJoinTable();
			if ( joinTable != null ) {
				attributeOverrideAnn.setAttributeValue(
						"joinTable",
						TableProcessing.createNestedJoinTable(
								joinTable,
								mutableClassDetails,
								xmlDocumentContext
						)
				);
			}
		}

		final MutableAnnotationUsage<AssociationOverrides> associationOverridesAnn = XmlProcessingHelper.getOrMakeAnnotation(
				AssociationOverrides.class,
				mutableClassDetails,
				xmlDocumentContext
		);
		associationOverridesAnn.setAttributeValue( "value", associationOverrideList );
	}
}
