/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.internal.Abstract;
import org.hibernate.boot.internal.Extends;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityOrMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedEntityGraphImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTenantIdImpl;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.xml.internal.attr.BasicAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.BasicIdAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.EmbeddedIdAttributeProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.boot.models.xml.spi.XmlProcessingResult;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.ModelsException;
import org.hibernate.models.internal.ModelsClassLogging;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.MapModeFieldDetails;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.NullnessHelper.nullif;

/**
 * Helper for handling managed types defined in mapping XML, in either
 * metadata-complete or override mode
 *
 * @author Steve Ebersole
 */
public class ManagedTypeProcessor {
	private static final int MEMBER_MODIFIERS = buildMemberModifiers();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity

	public static void processCompleteEntity(
			JaxbEntityMappingsImpl jaxbRoot,
			JaxbEntityImpl jaxbEntity,
			XmlDocumentContext xmlDocumentContext) {
		final MutableClassDetails classDetails;
		final AccessType classAccessType;
		final AttributeProcessor.MemberAdjuster memberAdjuster;

		final ClassDetailsRegistry classDetailsRegistry = xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry();

		if ( StringHelper.isEmpty( jaxbEntity.getClazz() ) ) {
			// no class == dynamic
			if ( StringHelper.isEmpty( jaxbEntity.getName() ) ) {
				throw new ModelsException( "Assumed dynamic entity did not define entity-name" );
			}

			memberAdjuster = ManagedTypeProcessor::adjustDynamicTypeMember;
			classAccessType = AccessType.FIELD;
			classDetails = (MutableClassDetails) classDetailsRegistry.resolveClassDetails(
					jaxbEntity.getName(),
					(name) -> new DynamicClassDetails(
							jaxbEntity.getName(),
							null,
							false,
							null,
							xmlDocumentContext.getModelBuildingContext()
					)
			);

			prepareDynamicClass( classDetails, jaxbEntity, xmlDocumentContext );
		}
		else {
			memberAdjuster = ManagedTypeProcessor::adjustNonDynamicTypeMember;
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEntity );
			classDetails = (MutableClassDetails) classDetailsRegistry.resolveClassDetails( className );
			classAccessType = coalesce(
					jaxbEntity.getAccess(),
					jaxbRoot.getAccess(),
					xmlDocumentContext.getPersistenceUnitMetadata().getAccessType(),
					AccessType.PROPERTY
			);
		}

		classDetails.clearMemberAnnotationUsages();
		classDetails.clearAnnotationUsages();

		// from here, processing is the same between override and metadata-complete modes (aside from the dynamic model handling)

		processEntityMetadata(
				classDetails,
				jaxbEntity,
				classAccessType,
				memberAdjuster,
				jaxbRoot,
				xmlDocumentContext
		);
	}

	/**
	 * Creates fake FieldDetails for each attribute defined in the XML
	 */
	private static void prepareDynamicClass(
			MutableClassDetails classDetails,
			JaxbManagedType jaxbManagedType,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbManagedType instanceof JaxbEntityImpl jaxbDynamicEntity ) {
			final JaxbAttributesContainerImpl attributes = jaxbDynamicEntity.getAttributes();

			if ( CollectionHelper.isNotEmpty( attributes.getIdAttributes() ) ) {
				// <id/>
				attributes.getIdAttributes().forEach( (jaxbId) -> {
					final ClassDetails attributeJavaType = determineDynamicAttributeJavaType( jaxbId, xmlDocumentContext );
					final MapModeFieldDetails member = new MapModeFieldDetails(
							jaxbId.getName(),
							attributeJavaType,
							MEMBER_MODIFIERS,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				} );
			}
			else {
				// <embedded-id/>
				final JaxbEmbeddedIdImpl embeddedId = attributes.getEmbeddedIdAttribute();
				final ClassDetails attributeJavaType = determineDynamicAttributeJavaType( embeddedId, xmlDocumentContext );
				final MapModeFieldDetails member = new MapModeFieldDetails(
						embeddedId.getName(),
						attributeJavaType,
						MEMBER_MODIFIERS,
						xmlDocumentContext.getModelBuildingContext()
				);
				classDetails.addField( member );
			}

			// <natural-id/>
			if ( attributes.getNaturalId() != null ) {
				attributes.getNaturalId().getBasicAttributes().forEach( (jaxbBasic) -> {
					final ClassDetails attributeJavaType = determineDynamicAttributeJavaType(
							jaxbBasic,
							xmlDocumentContext
					);
					final MapModeFieldDetails member = new MapModeFieldDetails(
							jaxbBasic.getName(),
							attributeJavaType,
							MEMBER_MODIFIERS,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				} );

				attributes.getNaturalId().getEmbeddedAttributes().forEach( (jaxbEmbedded) -> {
					final ClassDetails attributeJavaType = determineDynamicAttributeJavaType(
							jaxbEmbedded,
							xmlDocumentContext
					);
					final MapModeFieldDetails member = new MapModeFieldDetails(
							jaxbEmbedded.getName(),
							attributeJavaType,
							MEMBER_MODIFIERS,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				} );

				attributes.getNaturalId().getManyToOneAttributes().forEach( (jaxbManyToOne) -> {
					final ClassDetails attributeJavaType = determineDynamicAttributeJavaType( jaxbManyToOne, xmlDocumentContext );
					final MapModeFieldDetails member = new MapModeFieldDetails(
							jaxbManyToOne.getName(),
							attributeJavaType,
							MEMBER_MODIFIERS,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				} );

				attributes.getNaturalId().getAnyMappingAttributes().forEach( (jaxbAnyMapping) -> {
					final ClassDetails attributeJavaType = determineDynamicAttributeJavaType( jaxbAnyMapping, xmlDocumentContext );
					final MapModeFieldDetails member = new MapModeFieldDetails(
							jaxbAnyMapping.getName(),
							attributeJavaType,
							MEMBER_MODIFIERS,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				} );
			}

			// <tenant-id>
			final JaxbTenantIdImpl tenantId = jaxbDynamicEntity.getTenantId();
			if ( tenantId != null ) {
				final ClassDetails attributeJavaType = determineDynamicAttributeJavaType(
						tenantId,
						xmlDocumentContext
				);
				final MapModeFieldDetails member = new MapModeFieldDetails(
						tenantId.getName(),
						attributeJavaType,
						MEMBER_MODIFIERS,
						xmlDocumentContext.getModelBuildingContext()
				);
				classDetails.addField( member );
			}
		}

		final JaxbAttributesContainer attributes = jaxbManagedType.getAttributes();
		attributes.getBasicAttributes().forEach( (jaxbBasic) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbBasic.getName(),
					determineDynamicAttributeJavaType( jaxbBasic, xmlDocumentContext ),
					MEMBER_MODIFIERS,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		attributes.getEmbeddedAttributes().forEach( (jaxbEmbedded) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbEmbedded.getName(),
					determineDynamicAttributeJavaType( jaxbEmbedded, xmlDocumentContext ),
					MEMBER_MODIFIERS,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		attributes.getOneToOneAttributes().forEach( (jaxbOneToOne) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbOneToOne.getName(),
					determineDynamicAttributeJavaType( jaxbOneToOne, xmlDocumentContext ),
					MEMBER_MODIFIERS,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		attributes.getManyToOneAttributes().forEach( (jaxbManyToOne) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbManyToOne.getName(),
					determineDynamicAttributeJavaType( jaxbManyToOne, xmlDocumentContext ),
					MEMBER_MODIFIERS,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		attributes.getAnyMappingAttributes().forEach( (jaxbAnyMapping) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbAnyMapping.getName(),
					determineDynamicAttributeJavaType( jaxbAnyMapping, xmlDocumentContext ),
					MEMBER_MODIFIERS,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		attributes.getElementCollectionAttributes().forEach( (jaxbElementCollection) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbElementCollection.getName(),
					determineDynamicAttributeJavaType( jaxbElementCollection, xmlDocumentContext ),
					MEMBER_MODIFIERS,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		attributes.getOneToManyAttributes().forEach( (jaxbOneToMany) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbOneToMany.getName(),
					determineDynamicAttributeJavaType( jaxbOneToMany, xmlDocumentContext ),
					MEMBER_MODIFIERS,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		attributes.getManyToManyAttributes().forEach( (jaxbManyToMany) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbManyToMany.getName(),
					determineDynamicAttributeJavaType( jaxbManyToMany, xmlDocumentContext ),
					MEMBER_MODIFIERS,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		attributes.getPluralAnyMappingAttributes().forEach( (jaxbPluralAnyMapping) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbPluralAnyMapping.getName(),
					determineDynamicAttributeJavaType( jaxbPluralAnyMapping, xmlDocumentContext ),
					MEMBER_MODIFIERS,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );
	}

	private static ClassDetails determineDynamicAttributeJavaType(
			JaxbPersistentAttribute jaxbPersistentAttribute,
			XmlDocumentContext xmlDocumentContext) {
		final ClassDetailsRegistry classDetailsRegistry = xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry();

		if ( jaxbPersistentAttribute instanceof JaxbIdImpl jaxbId ) {
			return xmlDocumentContext.resolveJavaType( jaxbId.getTarget() );
		}

		if ( jaxbPersistentAttribute instanceof JaxbEmbeddedIdImpl jaxbEmbeddedId ) {
			final String target = jaxbEmbeddedId.getTarget();
			if ( StringHelper.isEmpty( target ) ) {
				return null;
			}
			return classDetailsRegistry.resolveClassDetails(
					target,
					(name) -> new DynamicClassDetails( target, xmlDocumentContext.getModelBuildingContext() )
			);
		}

		if ( jaxbPersistentAttribute instanceof JaxbBasicImpl jaxbBasic ) {
			return xmlDocumentContext.resolveJavaType( jaxbBasic.getTarget() );
		}

		if ( jaxbPersistentAttribute instanceof JaxbEmbeddedImpl jaxbEmbedded ) {
			final String target = jaxbEmbedded.getTarget();
			if ( StringHelper.isEmpty( target ) ) {
				return null;
			}
			return classDetailsRegistry.resolveClassDetails(
					target,
					(name) -> new DynamicClassDetails( target, xmlDocumentContext.getModelBuildingContext() )
			);
		}

		if ( jaxbPersistentAttribute instanceof JaxbOneToOneImpl jaxbOneToOne ) {
			final String target = jaxbOneToOne.getTargetEntity();
			if ( StringHelper.isEmpty( target ) ) {
				return null;
			}
			return classDetailsRegistry.resolveClassDetails(
					target,
					(name) -> new DynamicClassDetails(
							target,
							null,
							false,
							null,
							xmlDocumentContext.getModelBuildingContext()
					)
			);
		}

		if ( jaxbPersistentAttribute instanceof JaxbAnyMappingImpl ) {
			return classDetailsRegistry.getClassDetails( Object.class.getName() );
		}

		if ( jaxbPersistentAttribute instanceof JaxbPluralAttribute jaxbPluralAttribute ) {
			final LimitedCollectionClassification classification = nullif( jaxbPluralAttribute.getClassification(), LimitedCollectionClassification.BAG );
			return switch ( classification ) {
				case BAG -> classDetailsRegistry.resolveClassDetails( Collection.class.getName() );
				case LIST -> classDetailsRegistry.resolveClassDetails( List.class.getName() );
				case SET -> classDetailsRegistry.resolveClassDetails( Set.class.getName() );
				case MAP -> classDetailsRegistry.resolveClassDetails( Map.class.getName() );
			};
		}
		throw new UnsupportedOperationException( "Resolution of dynamic attribute Java type not yet implemented for " + jaxbPersistentAttribute );
	}

	private static void adjustDynamicTypeMember(
			MutableMemberDetails memberDetails,
			JaxbPersistentAttribute jaxbAttribute,
			XmlDocumentContext xmlDocumentContext) {
		XmlAnnotationHelper.applyAttributeAccessor( BuiltInPropertyAccessStrategies.MAP.getExternalName(), memberDetails, xmlDocumentContext );
	}

	private static void processEntityMetadata(
			MutableClassDetails classDetails,
			JaxbEntityImpl jaxbEntity,
			AccessType classAccessType,
			AttributeProcessor.MemberAdjuster memberAdjuster,
			JaxbEntityMappingsImpl jaxbRoot,
			XmlDocumentContext xmlDocumentContext) {
		XmlAnnotationHelper.applyEntity( jaxbEntity, classDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyInheritance( jaxbEntity, classDetails, xmlDocumentContext );
		classDetails.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, classDetails, xmlDocumentContext ) );

		if ( jaxbEntity.isAbstract() != null ) {
			XmlProcessingHelper.makeAnnotation( Abstract.class, classDetails, xmlDocumentContext );
		}

		if ( StringHelper.isNotEmpty( jaxbEntity.getExtends() ) ) {
			final MutableAnnotationUsage<Extends> extendsAnn = XmlProcessingHelper.makeAnnotation(
					Extends.class,
					classDetails,
					xmlDocumentContext
			);
			extendsAnn.setAttributeValue( "superType", jaxbEntity.getExtends() );
		}

		if ( jaxbEntity.getTable() != null ) {
			XmlAnnotationHelper.applyTable( jaxbEntity.getTable(), classDetails, xmlDocumentContext );
		}

		final JaxbAttributesContainerImpl attributes = jaxbEntity.getAttributes();
		if ( attributes != null ) {
			processIdMappings(
					attributes,
					classAccessType,
					classDetails,
					memberAdjuster,
					xmlDocumentContext
			);
			AttributeProcessor.processNaturalId(
					attributes.getNaturalId(),
					classDetails,
					classAccessType,
					memberAdjuster,
					xmlDocumentContext
			);
			AttributeProcessor.processAttributes(
					attributes,
					classDetails,
					classAccessType,
					memberAdjuster,
					xmlDocumentContext
			);
		}

		jaxbEntity.getFilters().forEach( jaxbFilter -> XmlAnnotationHelper.applyFilter(
				jaxbFilter,
				classDetails,
				xmlDocumentContext
		) );

		XmlAnnotationHelper.applySqlRestriction( jaxbEntity.getSqlRestriction(), classDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyCustomSql( jaxbEntity.getSqlInsert(), classDetails, SQLInsert.class, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbEntity.getSqlUpdate(), classDetails, SQLUpdate.class, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbEntity.getSqlDelete(), classDetails, SQLDelete.class, xmlDocumentContext );

		processEntityOrMappedSuperclass( jaxbEntity, classDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyRowId( jaxbEntity.getRowid(), classDetails, xmlDocumentContext );

		applyTenantId( classDetails, jaxbEntity, classAccessType, xmlDocumentContext );

		final List<JaxbNamedEntityGraphImpl> namedEntityGraphs = jaxbEntity.getNamedEntityGraphs();
		for ( JaxbNamedEntityGraphImpl namedEntityGraph : namedEntityGraphs ) {
			XmlAnnotationHelper.applyNamedEntityGraph( namedEntityGraph, classDetails, xmlDocumentContext );
		}

		XmlAnnotationHelper.applyDiscriminatorValue(
				jaxbEntity.getDiscriminatorValue(),
				classDetails,
				xmlDocumentContext
		);

		XmlAnnotationHelper.applyDiscriminatorColumn(
				jaxbEntity.getDiscriminatorColumn(),
				classDetails,
				xmlDocumentContext
		);

		XmlAnnotationHelper.applyDiscriminatorFormula(
				jaxbEntity.getDiscriminatorFormula(),
				classDetails,
				xmlDocumentContext
		);
		// todo : secondary-tables
	}

	private static void applyTenantId(
			MutableClassDetails classDetails,
			JaxbEntityImpl jaxbEntity,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final JaxbTenantIdImpl jaxbTenantId = jaxbEntity.getTenantId();
		if ( jaxbTenantId != null ) {
			final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
					jaxbTenantId.getName(),
					coalesce( jaxbTenantId.getAccess(), classAccessType ),
					classDetails
			);
			XmlProcessingHelper.getOrMakeAnnotation(
					TenantId.class,
					memberDetails,
					xmlDocumentContext
			);
			BasicAttributeProcessing.processBasicAttribute(
					jaxbTenantId,
					classDetails,
					classAccessType,
					xmlDocumentContext
			);
			// todo : add bind-as-param attribute to @TenantId
		}
	}


	private static void adjustNonDynamicTypeMember(
			MutableMemberDetails memberDetails,
			JaxbPersistentAttribute jaxbAttribute,
			XmlDocumentContext xmlDocumentContext) {
		CommonAttributeProcessing.applyAttributeAccessor(
				jaxbAttribute,
				memberDetails,
				xmlDocumentContext
		);
	}

	public static void processOverrideEntity(List<XmlProcessingResult.OverrideTuple<JaxbEntityImpl>> entityOverrides) {
		entityOverrides.forEach( (overrideTuple) -> {
			final XmlDocumentContext xmlDocumentContext = overrideTuple.getXmlDocumentContext();
			final JaxbEntityMappingsImpl jaxbRoot = overrideTuple.getJaxbRoot();
			final JaxbEntityImpl jaxbEntity = overrideTuple.getManagedType();
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEntity );
			final MutableClassDetails classDetails = (MutableClassDetails) xmlDocumentContext
					.getModelBuildingContext()
					.getClassDetailsRegistry()
					.resolveClassDetails( className );

			final AccessType classAccessType = coalesceSuppliedValues(
					jaxbEntity::getAccess,
					jaxbRoot::getAccess,
					() -> determineAccessTypeFromClassAnnotations( classDetails ),
					xmlDocumentContext.getPersistenceUnitMetadata()::getAccessType,
					() -> AccessType.PROPERTY
			);

			// from here, processing is the same between override and metadata-complete modes
			processEntityMetadata(
					classDetails,
					jaxbEntity,
					classAccessType,
					ManagedTypeProcessor::adjustNonDynamicTypeMember,
					jaxbRoot,
					xmlDocumentContext
			);
		} );

	}

	private static AccessType determineAccessTypeFromClassAnnotations(ClassDetails classDetails) {
		final AnnotationUsage<Access> accessUsage = classDetails.getAnnotationUsage( Access.class );
		if ( accessUsage != null ) {
			return accessUsage.getAttributeValue( "value" );
		}

		for ( FieldDetails field : classDetails.getFields() ) {
			if ( field.getAnnotationUsage( Id.class ) != null
					|| field.getAnnotationUsage( EmbeddedId.class ) != null ) {
				return AccessType.FIELD;
			}
		}

		for ( MethodDetails method : classDetails.getMethods() ) {
			if ( method.getAnnotationUsage( Id.class ) != null
					|| method.getAnnotationUsage( EmbeddedId.class ) != null ) {
				assert method.getMethodKind() == MethodDetails.MethodKind.GETTER;
				return AccessType.PROPERTY;
			}
		}

		return null;
	}

	/**
	 * Used in cases where neither the class nor the XML defined an explicit AccessType.
	 * </p>
	 * According to the specification, strictly speaking, this should (could) be an exception
	 */
	private static Supplier<AccessType> determineAccessTypeFromClassAndXml(
			JaxbEntityImpl jaxbEntity,
			MutableClassDetails classDetails) {
		return null;
	}

	private static void processIdMappings(
			JaxbAttributesContainerImpl attributes,
			AccessType classAccessType,
			MutableClassDetails classDetails,
			AttributeProcessor.MemberAdjuster memberAdjuster,
			XmlDocumentContext xmlDocumentContext) {
		final List<JaxbIdImpl> jaxbIds = attributes.getIdAttributes();
		final JaxbEmbeddedIdImpl jaxbEmbeddedId = attributes.getEmbeddedIdAttribute();

		if ( CollectionHelper.isNotEmpty( jaxbIds ) ) {
			for ( int i = 0; i < jaxbIds.size(); i++ ) {
				final JaxbIdImpl jaxbId = jaxbIds.get( i );
				final MutableMemberDetails memberDetails = BasicIdAttributeProcessing.processBasicIdAttribute(
						jaxbId,
						classDetails,
						classAccessType,
						xmlDocumentContext
				);
				memberAdjuster.adjust( memberDetails, jaxbId, xmlDocumentContext );
			}
		}
		else if ( jaxbEmbeddedId != null ) {
			final MutableMemberDetails memberDetails = EmbeddedIdAttributeProcessing.processEmbeddedIdAttribute(
					jaxbEmbeddedId,
					classDetails,
					classAccessType,
					xmlDocumentContext
			);
			memberAdjuster.adjust( memberDetails, jaxbEmbeddedId, xmlDocumentContext );
		}
		else {
			ModelsClassLogging.MODELS_CLASS_LOGGER.debugf(
					"Identifiable type [%s] contained no <id/> nor <embedded-id/>",
					classDetails.getName()
			);
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// MappedSuperclass

	public static void processCompleteMappedSuperclass(
			JaxbEntityMappingsImpl jaxbRoot,
			JaxbMappedSuperclassImpl jaxbMappedSuperclass,
			XmlDocumentContext xmlDocumentContext) {
		// todo : should we allow mapped-superclass in dynamic models?
		//		that would need a change in XSD

		final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbMappedSuperclass );
		final MutableClassDetails classDetails = (MutableClassDetails) xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( className );

		classDetails.clearMemberAnnotationUsages();
		classDetails.clearAnnotationUsages();

		processMappedSuperclassMetadata( jaxbMappedSuperclass, classDetails, xmlDocumentContext );
	}

	private static void processMappedSuperclassMetadata(
			JaxbMappedSuperclassImpl jaxbMappedSuperclass,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		XmlProcessingHelper.getOrMakeAnnotation( MappedSuperclass.class, classDetails, xmlDocumentContext );

		final AccessType classAccessType = coalesce(
				jaxbMappedSuperclass.getAccess(),
				xmlDocumentContext.getPersistenceUnitMetadata().getAccessType()
		);
		classDetails.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, classDetails, xmlDocumentContext ) );

		final JaxbAttributesContainer attributes = jaxbMappedSuperclass.getAttributes();
		AttributeProcessor.processAttributes( attributes, classDetails, classAccessType, xmlDocumentContext );

		processEntityOrMappedSuperclass( jaxbMappedSuperclass, classDetails, xmlDocumentContext );
	}

	public static void processOverrideMappedSuperclass(List<XmlProcessingResult.OverrideTuple<JaxbMappedSuperclassImpl>> mappedSuperclassesOverrides) {
		mappedSuperclassesOverrides.forEach( (overrideTuple) -> {
			final XmlDocumentContext xmlDocumentContext = overrideTuple.getXmlDocumentContext();
			final JaxbEntityMappingsImpl jaxbRoot = overrideTuple.getJaxbRoot();
			final JaxbMappedSuperclassImpl jaxbMappedSuperclass = overrideTuple.getManagedType();
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbMappedSuperclass );
			final MutableClassDetails classDetails = (MutableClassDetails) xmlDocumentContext
					.getModelBuildingContext()
					.getClassDetailsRegistry()
					.resolveClassDetails( className );

			processMappedSuperclassMetadata( jaxbMappedSuperclass, classDetails, xmlDocumentContext );
		} );
	}

	private static void processEntityOrMappedSuperclass(
			JaxbEntityOrMappedSuperclass jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		XmlAnnotationHelper.applyIdClass( jaxbEntity.getIdClass(), classDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyLifecycleCallbacks(
				jaxbEntity,
				JpaEventListenerStyle.CALLBACK,
				classDetails,
				xmlDocumentContext
		);

		if ( jaxbEntity.getEntityListenerContainer() != null ) {
			jaxbEntity.getEntityListenerContainer().getEntityListeners().forEach( ( jaxbEntityListener -> {
				XmlAnnotationHelper.applyEntityListener( jaxbEntityListener, classDetails, xmlDocumentContext );
			} ) );
		}
	}

	public static void processCompleteEmbeddable(
			JaxbEntityMappingsImpl jaxbRoot,
			JaxbEmbeddableImpl jaxbEmbeddable,
			XmlDocumentContext xmlDocumentContext) {
		final MutableClassDetails classDetails;
		final AccessType classAccessType;
		final AttributeProcessor.MemberAdjuster memberAdjuster;

		final ClassDetailsRegistry classDetailsRegistry = xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry();

		if ( StringHelper.isEmpty( jaxbEmbeddable.getClazz() ) ) {
			if ( StringHelper.isEmpty( jaxbEmbeddable.getName() ) ) {
				throw new ModelsException( "Embeddable did not define class nor name" );
			}
			// no class == dynamic...
			classDetails = (MutableClassDetails) classDetailsRegistry
					.resolveClassDetails( jaxbEmbeddable.getName(), DynamicClassDetails::new );
			classAccessType = AccessType.FIELD;
			memberAdjuster = ManagedTypeProcessor::adjustDynamicTypeMember;

			prepareDynamicClass( classDetails, jaxbEmbeddable, xmlDocumentContext );
		}
		else {
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEmbeddable );
			classDetails = (MutableClassDetails) classDetailsRegistry.resolveClassDetails( className );
			classAccessType = coalesce(
					jaxbEmbeddable.getAccess(),
					xmlDocumentContext.getPersistenceUnitMetadata().getAccessType()
			);
			memberAdjuster = ManagedTypeProcessor::adjustNonDynamicTypeMember;
		}

		classDetails.clearMemberAnnotationUsages();
		classDetails.clearAnnotationUsages();

		processEmbeddableMetadata(
				jaxbEmbeddable,
				classDetails,
				classAccessType,
				memberAdjuster,
				xmlDocumentContext
		);
	}

	private static void processEmbeddableMetadata(
			JaxbEmbeddableImpl jaxbEmbeddable,
			MutableClassDetails classDetails,
			AccessType classAccessType,
			AttributeProcessor.MemberAdjuster memberAdjuster,
			XmlDocumentContext xmlDocumentContext) {
		XmlProcessingHelper.getOrMakeAnnotation( Embeddable.class, classDetails, xmlDocumentContext );
		classDetails.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, classDetails, xmlDocumentContext ) );

		AttributeProcessor.processAttributes(
				jaxbEmbeddable.getAttributes(),
				classDetails,
				AccessType.FIELD,
				memberAdjuster,
				xmlDocumentContext
		);
	}

	public static void processOverrideEmbeddable(List<XmlProcessingResult.OverrideTuple<JaxbEmbeddableImpl>> embeddableOverrides) {
		embeddableOverrides.forEach( (overrideTuple) -> {
			final XmlDocumentContext xmlDocumentContext = overrideTuple.getXmlDocumentContext();
			final JaxbEntityMappingsImpl jaxbRoot = overrideTuple.getJaxbRoot();
			final JaxbEmbeddableImpl jaxbEmbeddable = overrideTuple.getManagedType();
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEmbeddable );
			final MutableClassDetails classDetails = (MutableClassDetails) xmlDocumentContext
					.getModelBuildingContext()
					.getClassDetailsRegistry()
					.resolveClassDetails( className );

			AttributeProcessor.processAttributes(
					jaxbEmbeddable.getAttributes(),
					classDetails,
					AccessType.FIELD,
					ManagedTypeProcessor::adjustNonDynamicTypeMember,
					xmlDocumentContext
			);
		} );
	}

	private static int buildMemberModifiers() {
		return Modifier.fieldModifiers();
	}
}
