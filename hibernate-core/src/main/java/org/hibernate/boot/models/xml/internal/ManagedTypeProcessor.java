/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCachingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityOrMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTenantIdImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.XmlAnnotations;
import org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.AttributeAccessorAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ExtendsXmlAnnotation;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.boot.models.xml.internal.attr.BasicAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.BasicIdAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing;
import org.hibernate.boot.models.xml.internal.attr.EmbeddedIdAttributeProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.boot.models.xml.spi.XmlProcessingResult;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.ModelsException;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.ModelsClassLogging;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.rendering.internal.RenderingTargetCollectingImpl;
import org.hibernate.models.rendering.internal.SimpleRenderer;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;

import static org.hibernate.boot.models.xml.XmlProcessLogging.XML_PROCESS_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * Helper for handling managed types defined in mapping XML, in either
 * metadata-complete or override mode
 *
 * @author Steve Ebersole
 */
public class ManagedTypeProcessor {

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
			classDetails = (MutableClassDetails) ModelsHelper.resolveClassDetails(
					jaxbEntity.getName(),
					classDetailsRegistry,
					() -> {
						final ClassDetails superClass;
						final TypeDetails superType;

						if ( StringHelper.isEmpty( jaxbEntity.getExtends() ) ) {
							superClass = null;
							superType = null;
						}
						else {
							// we expect the super to have been processed first.
							// not worth the effort to support the delay
							superClass = classDetailsRegistry.getClassDetails( jaxbEntity.getExtends() );
							superType = new ClassTypeDetailsImpl( superClass, TypeDetails.Kind.CLASS );
						}

						return new DynamicClassDetails(
								jaxbEntity.getName(),
								null,
								jaxbEntity.isAbstract() != null && jaxbEntity.isAbstract(),
								superClass,
								superType,
								xmlDocumentContext.getModelBuildingContext()
						);
					}
			);

			DynamicModelHelper.prepareDynamicClass( classDetails, jaxbEntity, xmlDocumentContext );
		}
		else {
			memberAdjuster = ManagedTypeProcessor::adjustCompleteNonDynamicTypeMember;
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEntity );
			classDetails = (MutableClassDetails) classDetailsRegistry.resolveClassDetails( className );
			classAccessType = coalesce(
					jaxbEntity.getAccess(),
					jaxbRoot.getAccess(),
					xmlDocumentContext.getEffectiveDefaults().getDefaultPropertyAccessType(),
					defaultAccessTypeFromDefaultAccessor( xmlDocumentContext ),
					AccessType.PROPERTY
			);
			if ( classDetails.isInterface() ) {
				throw new MappingException( "Only classes (not interfaces) may be mapped as @Entity : " + classDetails.getName() );
			}

			classDetails.forEachPersistableMember( memberDetails -> {
				final MutableMemberDetails mutableMemberDetails = (MutableMemberDetails) memberDetails;
				mutableMemberDetails.clearAnnotationUsages();
			} );
		}

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

	private static AccessType defaultAccessTypeFromDefaultAccessor(XmlDocumentContext xmlDocumentContext) {
		final String defaultAccessStrategyName = xmlDocumentContext.getEffectiveDefaults().getDefaultAccessStrategyName();
		if ( BuiltInPropertyAccessStrategies.BASIC.getExternalName().equalsIgnoreCase( defaultAccessStrategyName )
				|| BuiltInPropertyAccessStrategies.BASIC.getStrategy().getClass().getName().equals( defaultAccessStrategyName ) ) {
			return AccessType.PROPERTY;
		}

		if ( BuiltInPropertyAccessStrategies.FIELD.getExternalName().equalsIgnoreCase( defaultAccessStrategyName )
				|| BuiltInPropertyAccessStrategies.FIELD.getStrategy().getClass().getName().equals( defaultAccessStrategyName ) ) {
			return AccessType.FIELD;
		}

		return null;
	}

	private static void adjustDynamicTypeMember(
			MutableMemberDetails memberDetails,
			JaxbPersistentAttribute jaxbAttribute,
			XmlDocumentContext xmlDocumentContext) {
		final AttributeAccessorAnnotation annotationUsage = (AttributeAccessorAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.ATTRIBUTE_ACCESSOR,
				xmlDocumentContext.getModelBuildingContext()
		);
		// todo (7.0) : this is the old String-based, deprecated form
		annotationUsage.value( BuiltInPropertyAccessStrategies.MAP.getExternalName() );
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
		applyAccessAnnotation( classAccessType, classDetails, xmlDocumentContext );
		applyCaching( jaxbEntity, classDetails, xmlDocumentContext );

		if ( jaxbEntity.isAbstract() != null ) {
			classDetails.applyAnnotationUsage( XmlAnnotations.ABSTRACT, xmlDocumentContext.getModelBuildingContext() );
		}

		if ( isNotEmpty( jaxbEntity.getExtends() ) ) {
			final ExtendsXmlAnnotation extendsAnn = XmlAnnotations.EXTENDS.createUsage( xmlDocumentContext.getModelBuildingContext() );
			extendsAnn.superType( jaxbEntity.getExtends() );
		}

		XmlAnnotationHelper.applyTable( jaxbEntity.getTable(), classDetails, xmlDocumentContext );
		XmlAnnotationHelper.applySecondaryTables( jaxbEntity.getSecondaryTables(), classDetails, xmlDocumentContext );

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
		AttributeProcessor.processAttributeOverrides(
				jaxbEntity.getAttributeOverrides(),
				classDetails,
				xmlDocumentContext
		);
		AttributeProcessor.processAssociationOverrides(
				jaxbEntity.getAssociationOverrides(),
				classDetails,
				xmlDocumentContext
		);

		QueryProcessing.applyNamedQueries( jaxbEntity, classDetails, xmlDocumentContext );
		QueryProcessing.applyNamedNativeQueries( jaxbEntity, classDetails, jaxbRoot, xmlDocumentContext );
		QueryProcessing.applyNamedProcedureQueries( jaxbEntity, classDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyFilters( jaxbEntity.getFilters(), classDetails, xmlDocumentContext );

		XmlAnnotationHelper.applySqlRestriction( jaxbEntity.getSqlRestriction(), classDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyCustomSql( jaxbEntity.getSqlInsert(), classDetails, HibernateAnnotations.SQL_INSERT, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbEntity.getSqlUpdate(), classDetails, HibernateAnnotations.SQL_UPDATE, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbEntity.getSqlDelete(), classDetails, HibernateAnnotations.SQL_DELETE, xmlDocumentContext );

		processEntityOrMappedSuperclass( jaxbEntity, classDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyRowId( jaxbEntity.getRowid(), classDetails, xmlDocumentContext );

		applyTenantId( classDetails, jaxbEntity, classAccessType, xmlDocumentContext );

		EntityGraphProcessing.applyEntityGraphs( jaxbEntity, classDetails, xmlDocumentContext );

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

		XmlAnnotationHelper.applyPrimaryKeyJoinColumns(
				jaxbEntity,
				classDetails,
				xmlDocumentContext
		);

		XmlAnnotationHelper.applyTableGenerator(
				jaxbEntity.getTableGenerator(),
				classDetails,
				xmlDocumentContext
		);

		XmlAnnotationHelper.applySequenceGenerator(
				jaxbEntity.getSequenceGenerator(),
				classDetails,
				xmlDocumentContext
		);

		renderClass( classDetails, xmlDocumentContext );
	}

	private static void renderClass(MutableClassDetails classDetails, XmlDocumentContext xmlDocumentContext) {
		if ( !XML_PROCESS_LOGGER.isDebugEnabled() ) {
			return;
		}

		final RenderingTargetCollectingImpl collectingTarget = new RenderingTargetCollectingImpl();
		final SimpleRenderer renderer = new SimpleRenderer( collectingTarget );
		renderer.renderClass( classDetails, xmlDocumentContext.getModelBuildingContext() );
		XML_PROCESS_LOGGER.tracef( "Class annotations from XML for %s:\n%s",
				classDetails.getName(),
				collectingTarget.toString() );
	}

	private static void applyAccessAnnotation(
			AccessType accessType,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		final AccessJpaAnnotation annotationUsage =
				(AccessJpaAnnotation)
						target.applyAnnotationUsage( JpaAnnotations.ACCESS,
								xmlDocumentContext.getModelBuildingContext() );
		annotationUsage.value( accessType );
		target.addAnnotationUsage( annotationUsage );
	}

	private static void applyCaching(
			JaxbEntityImpl jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbEntity.isCacheable() != null ) {
			final CacheableJpaAnnotation cacheableUsage =
					(CacheableJpaAnnotation)
							classDetails.applyAnnotationUsage( JpaAnnotations.CACHEABLE,
									xmlDocumentContext.getModelBuildingContext() );

			cacheableUsage.value( jaxbEntity.isCacheable() );
			classDetails.addAnnotationUsage( cacheableUsage );
		}

		final JaxbCachingImpl jaxbCaching = jaxbEntity.getCaching();
		if ( jaxbCaching != null ) {
			final CacheAnnotation cacheUsage =
					(CacheAnnotation)
							classDetails.replaceAnnotationUsage( HibernateAnnotations.CACHE,
									xmlDocumentContext.getModelBuildingContext() );
			if ( StringHelper.isNotEmpty( jaxbCaching.getRegion() ) ) {
				cacheUsage.region( jaxbCaching.getRegion() );
			}
			if ( jaxbCaching.getAccess() != null ) {
				final CacheConcurrencyStrategy strategy = convertCacheAccessType( jaxbCaching.getAccess() );
				if ( strategy != null ) {
					cacheUsage.usage( strategy );
				}
			}
		}
	}

	private static CacheConcurrencyStrategy convertCacheAccessType(org.hibernate.cache.spi.access.AccessType accessType) {
		return accessType == null ? null : CacheConcurrencyStrategy.fromAccessType( accessType );
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
			memberDetails.applyAnnotationUsage( HibernateAnnotations.TENANT_ID,
					xmlDocumentContext.getModelBuildingContext() );
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

	private static void adjustCompleteNonDynamicTypeMember(
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
			final MutableClassDetails classDetails =
					(MutableClassDetails)
							xmlDocumentContext.getModelBuildingContext()
									.getClassDetailsRegistry()
									.resolveClassDetails( className );

			final AccessType classAccessType = coalesceSuppliedValues(
					// look on this <entity/>
					jaxbEntity::getAccess,
					// look on the root <entity/>
					jaxbRoot::getAccess,
					// look for @Access on the entity class
					() -> determineAccessTypeFromClassAnnotations( classDetails ),
					// look for a default (PU metadata default) access
					xmlDocumentContext.getEffectiveDefaults()::getDefaultPropertyAccessType,
					// look at @Id/@EmbeddedId
					() -> determineAccessTypeFromClassMembers( classDetails ),
					// fallback to PROPERTY
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
		final Access accessUsage = classDetails.getDirectAnnotationUsage( Access.class );
		return accessUsage != null ? accessUsage.value() : null;

	}

	private static AccessType determineAccessTypeFromClassMembers(ClassDetails classDetails) {
		for ( FieldDetails field : classDetails.getFields() ) {
			if ( field.hasDirectAnnotationUsage( Id.class )
					|| field.hasDirectAnnotationUsage( EmbeddedId.class ) ) {
				return AccessType.FIELD;
			}
		}

		for ( MethodDetails method : classDetails.getMethods() ) {
			if ( method.hasDirectAnnotationUsage( Id.class )
					|| method.hasDirectAnnotationUsage( EmbeddedId.class ) ) {
				assert method.getMethodKind() == MethodDetails.MethodKind.GETTER;
				return AccessType.PROPERTY;
			}
		}

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

		if ( isNotEmpty( jaxbIds ) ) {
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
		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();

		classDetails.applyAnnotationUsage( JpaAnnotations.MAPPED_SUPERCLASS, modelBuildingContext );

		final AccessType classAccessType = coalesce(
				jaxbMappedSuperclass.getAccess(),
				xmlDocumentContext.getEffectiveDefaults().getDefaultPropertyAccessType()
		);
		if ( classAccessType != null ) {
			final AccessJpaAnnotation accessUsage = (AccessJpaAnnotation) classDetails.applyAnnotationUsage(
					JpaAnnotations.ACCESS,
					modelBuildingContext
			);
			accessUsage.value( classAccessType );
		}

		final JaxbAttributesContainerImpl attributes = jaxbMappedSuperclass.getAttributes();
		if ( attributes != null ) {
			processIdMappings(
					attributes,
					classAccessType,
					classDetails,
					ManagedTypeProcessor::adjustNonDynamicTypeMember,
					xmlDocumentContext
			);
			AttributeProcessor.processAttributes( attributes, classDetails, classAccessType, xmlDocumentContext );
		}

		processEntityOrMappedSuperclass( jaxbMappedSuperclass, classDetails, xmlDocumentContext );

		renderClass( classDetails, xmlDocumentContext );
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
			JaxbEntityOrMappedSuperclass jaxbClass,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		XmlAnnotationHelper.applyIdClass( jaxbClass.getIdClass(), classDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyLifecycleCallbacks( jaxbClass, classDetails, xmlDocumentContext );
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
			classDetails = (MutableClassDetails) ModelsHelper.resolveClassDetails(
					jaxbEmbeddable.getName(),
					classDetailsRegistry,
					() ->
							new DynamicClassDetails(
									jaxbEmbeddable.getName(),
									xmlDocumentContext.getModelBuildingContext()
							)
			);
			classAccessType = AccessType.FIELD;
			memberAdjuster = ManagedTypeProcessor::adjustDynamicTypeMember;

			DynamicModelHelper.prepareDynamicClass( classDetails, jaxbEmbeddable, xmlDocumentContext );
		}
		else {
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEmbeddable );
			classDetails = (MutableClassDetails) classDetailsRegistry.resolveClassDetails( className );
			classAccessType = coalesce(
					jaxbEmbeddable.getAccess(),
					xmlDocumentContext.getEffectiveDefaults().getDefaultPropertyAccessType()
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
		classDetails.applyAnnotationUsage( JpaAnnotations.EMBEDDABLE, xmlDocumentContext.getModelBuildingContext() );

		if ( classAccessType != null ) {
			final AccessJpaAnnotation accessUsage = (AccessJpaAnnotation) classDetails.applyAnnotationUsage(
					JpaAnnotations.ACCESS,
					xmlDocumentContext.getModelBuildingContext()
			);
			accessUsage.value( classAccessType );
		}

		if ( jaxbEmbeddable.getAttributes() != null ) {
			AttributeProcessor.processAttributes(
					jaxbEmbeddable.getAttributes(),
					classDetails,
					AccessType.FIELD,
					memberAdjuster,
					xmlDocumentContext
			);
		}
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

			classDetails.applyAnnotationUsage( JpaAnnotations.EMBEDDABLE, xmlDocumentContext.getModelBuildingContext() );

			if ( jaxbEmbeddable.getAttributes() != null ) {
				AttributeProcessor.processAttributes(
						jaxbEmbeddable.getAttributes(),
						classDetails,
						AccessType.FIELD,
						ManagedTypeProcessor::adjustNonDynamicTypeMember,
						xmlDocumentContext
				);
			}
		} );
	}
}
