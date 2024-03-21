/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.internal.Abstract;
import org.hibernate.boot.internal.Extends;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCachingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityOrMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedEntityGraphImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTenantIdImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
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
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.ModelsClassLogging;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

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
					final TypeDetails attributeJavaType = determineDynamicAttributeJavaType( jaxbId, xmlDocumentContext );
					final DynamicFieldDetails member = new DynamicFieldDetails(
							jaxbId.getName(),
							attributeJavaType,
							classDetails,
							MEMBER_MODIFIERS,
							false,
							false,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				} );
			}
			else {
				// <embedded-id/>
				final JaxbEmbeddedIdImpl embeddedId = attributes.getEmbeddedIdAttribute();
				final TypeDetails attributeJavaType = determineDynamicAttributeJavaType( embeddedId, xmlDocumentContext );
				final DynamicFieldDetails member = new DynamicFieldDetails(
						embeddedId.getName(),
						attributeJavaType,
						classDetails,
						MEMBER_MODIFIERS,
						false,
						false,
						xmlDocumentContext.getModelBuildingContext()
				);
				classDetails.addField( member );
			}

			// <natural-id/>
			if ( attributes.getNaturalId() != null ) {
				attributes.getNaturalId().getBasicAttributes().forEach( (jaxbBasic) -> {
					final TypeDetails attributeJavaType = determineDynamicAttributeJavaType(
							jaxbBasic,
							xmlDocumentContext
					);
					final DynamicFieldDetails member = new DynamicFieldDetails(
							jaxbBasic.getName(),
							attributeJavaType,
							classDetails,
							MEMBER_MODIFIERS,
							false,
							false,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				} );

				attributes.getNaturalId().getEmbeddedAttributes().forEach( (jaxbEmbedded) -> {
					final TypeDetails attributeJavaType = determineDynamicAttributeJavaType(
							jaxbEmbedded,
							xmlDocumentContext
					);
					final DynamicFieldDetails member = new DynamicFieldDetails(
							jaxbEmbedded.getName(),
							attributeJavaType,
							classDetails,
							MEMBER_MODIFIERS,
							false,
							false,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				} );

				attributes.getNaturalId().getManyToOneAttributes().forEach( (jaxbManyToOne) -> {
					final TypeDetails attributeJavaType = determineDynamicAttributeJavaType( jaxbManyToOne, xmlDocumentContext );
					final DynamicFieldDetails member = new DynamicFieldDetails(
							jaxbManyToOne.getName(),
							attributeJavaType,
							classDetails,
							MEMBER_MODIFIERS,
							false,
							false,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				} );

				attributes.getNaturalId().getAnyMappingAttributes().forEach( (jaxbAnyMapping) -> {
					final TypeDetails attributeJavaType = determineDynamicAttributeJavaType( jaxbAnyMapping, xmlDocumentContext );
					final DynamicFieldDetails member = new DynamicFieldDetails(
							jaxbAnyMapping.getName(),
							attributeJavaType,
							classDetails,
							MEMBER_MODIFIERS,
							false,
							false,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				} );
			}

			// <tenant-id>
			final JaxbTenantIdImpl tenantId = jaxbDynamicEntity.getTenantId();
			if ( tenantId != null ) {
				final TypeDetails attributeJavaType = determineDynamicAttributeJavaType(
						tenantId,
						xmlDocumentContext
				);
				final DynamicFieldDetails member = new DynamicFieldDetails(
						tenantId.getName(),
						attributeJavaType,
						classDetails,
						MEMBER_MODIFIERS,
						false,
						false,
						xmlDocumentContext.getModelBuildingContext()
				);
				classDetails.addField( member );
			}
		}
		else if ( jaxbManagedType instanceof JaxbMappedSuperclassImpl jaxbMappedSuperclass ) {
			final JaxbAttributesContainerImpl attributes = jaxbMappedSuperclass.getAttributes();

			if ( CollectionHelper.isNotEmpty( attributes.getIdAttributes() ) ) {
				// <id/>
				attributes.getIdAttributes().forEach( (jaxbId) -> {
					final TypeDetails attributeJavaType = determineDynamicAttributeJavaType( jaxbId, xmlDocumentContext );
					final DynamicFieldDetails member = new DynamicFieldDetails(
							jaxbId.getName(),
							attributeJavaType,
							classDetails,
							MEMBER_MODIFIERS,
							false,
							false,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				} );
			}
			else {
				// <embedded-id/>
				final JaxbEmbeddedIdImpl embeddedId = attributes.getEmbeddedIdAttribute();
				final TypeDetails attributeJavaType = determineDynamicAttributeJavaType( embeddedId, xmlDocumentContext );
				final DynamicFieldDetails member = new DynamicFieldDetails(
						embeddedId.getName(),
						attributeJavaType,
						classDetails,
						MEMBER_MODIFIERS,
						false,
						false,
						xmlDocumentContext.getModelBuildingContext()
				);
				classDetails.addField( member );
			}
		}

		final JaxbAttributesContainer attributes = jaxbManagedType.getAttributes();

		// <basic/>
		attributes.getBasicAttributes().forEach( (jaxbBasic) -> {
			final DynamicFieldDetails member = new DynamicFieldDetails(
					jaxbBasic.getName(),
					determineDynamicAttributeJavaType( jaxbBasic, xmlDocumentContext ),
					classDetails,
					MEMBER_MODIFIERS,
					false,
					false,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		// <embedded/>
		attributes.getEmbeddedAttributes().forEach( (jaxbEmbedded) -> {
			final DynamicFieldDetails member = new DynamicFieldDetails(
					jaxbEmbedded.getName(),
					determineDynamicAttributeJavaType( jaxbEmbedded, xmlDocumentContext ),
					classDetails,
					MEMBER_MODIFIERS,
					false,
					false,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		// <one-to-one/>
		attributes.getOneToOneAttributes().forEach( (jaxbOneToOne) -> {
			final DynamicFieldDetails member = new DynamicFieldDetails(
					jaxbOneToOne.getName(),
					determineDynamicAttributeJavaType( jaxbOneToOne, xmlDocumentContext ),
					classDetails,
					MEMBER_MODIFIERS,
					false,
					false,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		// <many-to-one/>
		attributes.getManyToOneAttributes().forEach( (jaxbManyToOne) -> {
			final DynamicFieldDetails member = new DynamicFieldDetails(
					jaxbManyToOne.getName(),
					determineDynamicAttributeJavaType( jaxbManyToOne, xmlDocumentContext ),
					classDetails,
					MEMBER_MODIFIERS,
					false,
					false,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		// <any/>
		attributes.getAnyMappingAttributes().forEach( (jaxbAnyMapping) -> {
			final DynamicFieldDetails member = new DynamicFieldDetails(
					jaxbAnyMapping.getName(),
					determineDynamicAttributeJavaType( jaxbAnyMapping, xmlDocumentContext ),
					classDetails,
					MEMBER_MODIFIERS,
					false,
					false,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		// <element-collection/>
		attributes.getElementCollectionAttributes().forEach( (jaxbElementCollection) -> {
			final DynamicFieldDetails member = new DynamicFieldDetails(
					jaxbElementCollection.getName(),
					determineDynamicAttributeJavaType( jaxbElementCollection, xmlDocumentContext ),
					classDetails,
					MEMBER_MODIFIERS,
					false,
					true,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		// <one-to-many/>
		attributes.getOneToManyAttributes().forEach( (jaxbOneToMany) -> {
			final DynamicFieldDetails member = new DynamicFieldDetails(
					jaxbOneToMany.getName(),
					determineDynamicAttributeJavaType( jaxbOneToMany, xmlDocumentContext ),
					classDetails,
					MEMBER_MODIFIERS,
					false,
					true,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		// <many-to-many/>
		attributes.getManyToManyAttributes().forEach( (jaxbManyToMany) -> {
			final DynamicFieldDetails member = new DynamicFieldDetails(
					jaxbManyToMany.getName(),
					determineDynamicAttributeJavaType( jaxbManyToMany, xmlDocumentContext ),
					classDetails,
					MEMBER_MODIFIERS,
					false,
					true,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );

		// <many-to-any/>
		attributes.getPluralAnyMappingAttributes().forEach( (jaxbPluralAnyMapping) -> {
			final TypeDetails attributeType = determineDynamicAttributeJavaType(
					jaxbPluralAnyMapping,
					xmlDocumentContext
			);
			final DynamicFieldDetails member = new DynamicFieldDetails(
					jaxbPluralAnyMapping.getName(),
					attributeType,
					classDetails,
					MEMBER_MODIFIERS,
					false,
					true,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addField( member );
		} );
	}

	private static TypeDetails determineDynamicAttributeJavaType(
			JaxbPersistentAttribute jaxbPersistentAttribute,
			XmlDocumentContext xmlDocumentContext) {
		final MutableClassDetails classDetails = xmlDocumentContext.resolveDynamicJavaType( jaxbPersistentAttribute );
		return new ClassTypeDetailsImpl( classDetails, TypeDetails.Kind.CLASS );
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
		applyAccessAnnotation( classAccessType, classDetails, xmlDocumentContext );
		applyCaching( jaxbEntity, classDetails, xmlDocumentContext );

		if ( jaxbEntity.isAbstract() != null ) {
			XmlProcessingHelper.makeAnnotation( Abstract.class, classDetails, xmlDocumentContext );
		}

		if ( isNotEmpty( jaxbEntity.getExtends() ) ) {
			final MutableAnnotationUsage<Extends> extendsAnn = HibernateAnnotations.EXTENDS.createUsage(
					classDetails,
					xmlDocumentContext.getModelBuildingContext()
			);
			extendsAnn.setAttributeValue( "superType", jaxbEntity.getExtends() );
		}

		XmlAnnotationHelper.applyTable( jaxbEntity.getTable(), classDetails, xmlDocumentContext );

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

		QueryProcessing.applyNamedQueries( jaxbEntity, classDetails, xmlDocumentContext );
		QueryProcessing.applyNamedNativeQueries( jaxbEntity, classDetails, jaxbRoot, xmlDocumentContext );
		QueryProcessing.applyNamedProcedureQueries( jaxbEntity, classDetails, xmlDocumentContext );

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

	private static void applyAccessAnnotation(
			AccessType accessType,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<Access> annotationUsage = XmlProcessingHelper.makeAnnotation( Access.class, target, xmlDocumentContext );
		annotationUsage.setAttributeValue( "value", accessType );
		target.addAnnotationUsage( annotationUsage );
	}

	private static void applyCaching(
			JaxbEntityImpl jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbEntity.isCacheable() == Boolean.TRUE ) {
			final MutableAnnotationUsage<Cacheable> cacheableUsage = JpaAnnotations.CACHEABLE.createUsage(
					classDetails,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addAnnotationUsage( cacheableUsage );
		}

		final JaxbCachingImpl jaxbCaching = jaxbEntity.getCaching();
		if ( jaxbCaching != null ) {
			final MutableAnnotationUsage<Cache> cacheableUsage = HibernateAnnotations.CACHE.createUsage(
					classDetails,
					xmlDocumentContext.getModelBuildingContext()
			);
			classDetails.addAnnotationUsage( cacheableUsage );
			XmlProcessingHelper.applyAttributeIfSpecified( "region", jaxbCaching.getRegion(), cacheableUsage );
			XmlProcessingHelper.applyAttributeIfSpecified(
					"usage",
					convertCacheAccessType( jaxbCaching.getAccess() ),
					cacheableUsage
			);
		}
	}

	private static CacheConcurrencyStrategy convertCacheAccessType(org.hibernate.cache.spi.access.AccessType accessType) {
		if ( accessType == null ) {
			return null;
		}
		return CacheConcurrencyStrategy.fromAccessType( accessType );
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
					// look on this <entity/>
					jaxbEntity::getAccess,
					// look on the root <entity/>
					jaxbRoot::getAccess,
					// look for @Access on the entity class
					() -> determineAccessTypeFromClassAnnotations( classDetails ),
					// look for a default (PU metadata default) access
					xmlDocumentContext.getPersistenceUnitMetadata()::getAccessType,
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
		final AnnotationUsage<Access> accessUsage = classDetails.getAnnotationUsage( Access.class );
		if ( accessUsage != null ) {
			return accessUsage.getAttributeValue( "value" );
		}

		return null;
	}

	private static AccessType determineAccessTypeFromClassMembers(ClassDetails classDetails) {
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

		final JaxbAttributesContainerImpl attributes = jaxbMappedSuperclass.getAttributes();
		processIdMappings(
				attributes,
				classAccessType,
				classDetails,
				ManagedTypeProcessor::adjustNonDynamicTypeMember,
				xmlDocumentContext
		);
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

			XmlProcessingHelper.getOrMakeAnnotation( Embeddable.class, classDetails, xmlDocumentContext );

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
