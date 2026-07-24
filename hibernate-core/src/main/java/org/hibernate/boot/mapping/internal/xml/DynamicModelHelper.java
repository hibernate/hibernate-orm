/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.xml;

import jakarta.persistence.TemporalType;
import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAssociationAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddable;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.models.Creator;
import org.hibernate.models.spi.MutableClassDetailsRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.UserType;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import static org.hibernate.internal.util.NullnessHelper.nullif;
import static org.hibernate.internal.util.ReflectHelper.OBJECT_CLASS_NAME;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.models.Creator.DYNAMIC_ATTRIBUTE_MODIFIERS;

/**
 * Used from {@linkplain ManagedTypeProcessor} to help dealing with dynamic models
 *
 * @author Steve Ebersole
 */
public class DynamicModelHelper {
	/**
	 * Creates MutableMemberDetails for each attribute defined in the XML
	 */
	static void prepareDynamicClass(
			MutableClassDetails classDetails,
			JaxbManagedType jaxbManagedType,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbManagedType instanceof JaxbEntityImpl jaxbDynamicEntity ) {
			final var attributes = jaxbDynamicEntity.getAttributes();
			if ( attributes != null ) {
				if ( isNotEmpty( attributes.getIdAttributes() ) ) {
					// <id/>
					attributes.getIdAttributes().forEach( (jaxbId) -> {
						final TypeDetails attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbManagedType,
								jaxbId,
								xmlDocumentContext
						);
						final MutableMemberDetails member = Creator.createDynamicMemberDetails(
								jaxbId.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
								false,
								false,
								xmlDocumentContext.getModelsContext()
						);
						classDetails.addField( member.asFieldDetails() );
					} );
				}
				else if ( attributes.getEmbeddedIdAttribute() != null ) {
					// <embedded-id/>
					final var embeddedId = attributes.getEmbeddedIdAttribute();
					final var attributeJavaType = determineAttributeJavaTypeDetails(
							jaxbManagedType,
							embeddedId,
							xmlDocumentContext
					);
					final var member = Creator.createDynamicMemberDetails(
							embeddedId.getName(),
							attributeJavaType,
							classDetails,
							DYNAMIC_ATTRIBUTE_MODIFIERS,
							false,
							false,
							xmlDocumentContext.getModelsContext()
					);
					classDetails.addField( member.asFieldDetails() );
				}

				// <natural-id/>
				final var naturalId = attributes.getNaturalId();
				if ( naturalId != null ) {
					naturalId.getBasicAttributes().forEach( (jaxbBasic) -> {
						final var attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbManagedType,
								jaxbBasic,
								xmlDocumentContext
						);
						final var member = Creator.createDynamicMemberDetails(
								jaxbBasic.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
								false,
								false,
								xmlDocumentContext.getModelsContext()
						);
						classDetails.addField( member.asFieldDetails() );
					} );

					naturalId.getEmbeddedAttributes().forEach( (jaxbEmbedded) -> {
						final var attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbManagedType,
								jaxbEmbedded,
								xmlDocumentContext
						);
						final var member = Creator.createDynamicMemberDetails(
								jaxbEmbedded.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
								false,
								false,
								xmlDocumentContext.getModelsContext()
						);
						classDetails.addField( member.asFieldDetails() );
					} );

					naturalId.getManyToOneAttributes().forEach( (jaxbManyToOne) -> {
						final var attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbManyToOne,
								xmlDocumentContext
						);
						final var member = Creator.createDynamicMemberDetails(
								jaxbManyToOne.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
								false,
								false,
								xmlDocumentContext.getModelsContext()
						);
						classDetails.addField( member.asFieldDetails() );
					} );

					naturalId.getAnyMappingAttributes().forEach( (jaxbAnyMapping) -> {
						final var attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbAnyMapping,
								xmlDocumentContext
						);
						final var member = Creator.createDynamicMemberDetails(
								jaxbAnyMapping.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
								false,
								false,
								xmlDocumentContext.getModelsContext()
						);
						classDetails.addField( member.asFieldDetails() );
					} );
				}
			}

			// <tenant-id>
			final var tenantId = jaxbDynamicEntity.getTenantId();
			if ( tenantId != null ) {
				final var attributeJavaType = determineAttributeJavaTypeDetails(
						jaxbManagedType,
						tenantId,
						xmlDocumentContext
				);
				final var member = Creator.createDynamicMemberDetails(
						tenantId.getName(),
						attributeJavaType,
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						false,
						xmlDocumentContext.getModelsContext()
				);
				classDetails.addField( member.asFieldDetails() );
			}
		}
		else if ( jaxbManagedType instanceof JaxbMappedSuperclassImpl jaxbMappedSuperclass ) {
			final var attributes = jaxbMappedSuperclass.getAttributes();
			if ( attributes != null ) {
				if ( isNotEmpty( attributes.getIdAttributes() ) ) {
					// <id/>
					attributes.getIdAttributes().forEach( (jaxbId) -> {
						final var attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbManagedType,
								jaxbId,
								xmlDocumentContext
						);
						final var member = Creator.createDynamicMemberDetails(
								jaxbId.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
								false,
								false,
								xmlDocumentContext.getModelsContext()
						);
						classDetails.addField( member.asFieldDetails() );
					} );
				}
				else {
					// <embedded-id/>
					final var embeddedId = attributes.getEmbeddedIdAttribute();
					final var attributeJavaType = determineAttributeJavaTypeDetails(
							jaxbManagedType,
							embeddedId,
							xmlDocumentContext
					);
					final var member = Creator.createDynamicMemberDetails(
							embeddedId.getName(),
							attributeJavaType,
							classDetails,
							DYNAMIC_ATTRIBUTE_MODIFIERS,
							false,
							false,
							xmlDocumentContext.getModelsContext()
					);
					classDetails.addField( member.asFieldDetails() );
				}
			}
		}

		final JaxbAttributesContainer attributes = jaxbManagedType.getAttributes();

		if ( attributes != null ) {
			// <basic/>
			attributes.getBasicAttributes().forEach( (jaxbBasic) -> {
				final var member = Creator.createDynamicMemberDetails(
						jaxbBasic.getName(),
						determineAttributeJavaTypeDetails( jaxbManagedType, jaxbBasic, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						false,
						xmlDocumentContext.getModelsContext()
				);
				classDetails.addField( member.asFieldDetails() );
			} );

			// <embedded/>
			attributes.getEmbeddedAttributes().forEach( (jaxbEmbedded) -> {
				final var member = Creator.createDynamicMemberDetails(
						jaxbEmbedded.getName(),
						determineAttributeJavaTypeDetails( jaxbManagedType, jaxbEmbedded, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						false,
						xmlDocumentContext.getModelsContext()
				);
				classDetails.addField( member.asFieldDetails() );
			} );

			// <one-to-one/>
			attributes.getOneToOneAttributes().forEach( (jaxbOneToOne) -> {
				final var member = Creator.createDynamicMemberDetails(
						jaxbOneToOne.getName(),
						determineAttributeJavaTypeDetails( jaxbOneToOne, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						false,
						xmlDocumentContext.getModelsContext()
				);
				classDetails.addField( member.asFieldDetails() );
			} );

			// <many-to-one/>
			attributes.getManyToOneAttributes().forEach( (jaxbManyToOne) -> {
				final var member = Creator.createDynamicMemberDetails(
						jaxbManyToOne.getName(),
						determineAttributeJavaTypeDetails( jaxbManyToOne, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						false,
						xmlDocumentContext.getModelsContext()
				);
				classDetails.addField( member.asFieldDetails() );
			} );

			// <any/>
			attributes.getAnyMappingAttributes().forEach( (jaxbAnyMapping) -> {
				final var member = Creator.createDynamicMemberDetails(
						jaxbAnyMapping.getName(),
						determineAttributeJavaTypeDetails( jaxbAnyMapping, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						false,
						xmlDocumentContext.getModelsContext()
				);
				classDetails.addField( member.asFieldDetails() );
			} );

			// <element-collection/>
			attributes.getElementCollectionAttributes().forEach( (jaxbElementCollection) -> {
				final var elementType = determineAttributeJavaTypeDetails( jaxbElementCollection, xmlDocumentContext );
				final var member = Creator.createDynamicMemberDetails(
						jaxbElementCollection.getName(),
						makeCollectionType( classDetails, jaxbElementCollection, elementType, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						true,
						xmlDocumentContext.getModelsContext()
				);
				classDetails.addField( member.asFieldDetails() );
			} );

			// <one-to-many/>
			attributes.getOneToManyAttributes().forEach( (jaxbOneToMany) -> {
				final var elementType = determineAttributeJavaTypeDetails( jaxbOneToMany, xmlDocumentContext );
				final var member = Creator.createDynamicMemberDetails(
						jaxbOneToMany.getName(),
						// todo : this is wrong.  should be the collection-type (List, ...)
						//  	wrapping the result from determineAttributeJavaTypeDetails
						makeCollectionType( classDetails, jaxbOneToMany, elementType, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						true,
						xmlDocumentContext.getModelsContext()
				);
				classDetails.addField( member.asFieldDetails() );
			} );

			// <many-to-many/>
			attributes.getManyToManyAttributes().forEach( (jaxbManyToMany) -> {
				final var elementType = determineAttributeJavaTypeDetails( jaxbManyToMany, xmlDocumentContext );
				final var member = Creator.createDynamicMemberDetails(
						jaxbManyToMany.getName(),
						makeCollectionType( classDetails, jaxbManyToMany, elementType, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						true,
						xmlDocumentContext.getModelsContext()
				);
				classDetails.addField( member.asFieldDetails() );
			} );

			// <many-to-any/>
			attributes.getPluralAnyMappingAttributes().forEach( (jaxbPluralAnyMapping) -> {
				final var attributeType = determineAttributeJavaTypeDetails(
						jaxbPluralAnyMapping,
						xmlDocumentContext
				);
				final var member = Creator.createDynamicMemberDetails(
						jaxbPluralAnyMapping.getName(),
						attributeType,
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						true,
						xmlDocumentContext.getModelsContext()
				);
				classDetails.addField( member.asFieldDetails() );
			} );
		}
	}

	private static ClassDetails COLLECTION_CLASS_DETAILS;
	private static ClassDetails SET_CLASS_DETAILS;
	private static ClassDetails LIST_CLASS_DETAILS;
	private static ClassDetails SORTED_SET_CLASS_DETAILS;
	private static ClassDetails MAP_CLASS_DETAILS;
	private static ClassDetails SORTED_MAP_CLASS_DETAILS;

	private static TypeDetails makeCollectionType(
			ClassDetails declaringType,
			JaxbPluralAttribute jaxbPluralAttribute,
			TypeDetails elementType,
			XmlDocumentContext xmlDocumentContext) {
		final var classDetailsRegistry = xmlDocumentContext
				.getClassDetailsRegistry()
				.as( MutableClassDetailsRegistry.class );
		final ClassDetails collectionClassDetails;
		final List<TypeDetails> typeParams;

		switch ( jaxbPluralAttribute.getClassification() ) {
			case BAG -> {
				collectionClassDetails = collectionType( classDetailsRegistry );
				typeParams = List.of( elementType );
			}
			case LIST -> {
				collectionClassDetails = listType( classDetailsRegistry );
				typeParams = List.of( elementType );
			}
			case SET -> {
				collectionClassDetails = setType( jaxbPluralAttribute, classDetailsRegistry );
				typeParams = List.of( elementType );
			}
			case MAP -> {
				collectionClassDetails = mapType( jaxbPluralAttribute, classDetailsRegistry );
				// for now, just use wildcard for the key
				final ClassDetails objectClassDetails = classDetailsRegistry.resolveClassDetails( OBJECT_CLASS_NAME );
				typeParams = List.of(
						TypeDetails.extendsWildcard( TypeDetails.classType( objectClassDetails ) ),
						elementType
				);
			}
			default -> {
				throw new UnknownAttributeTypeException(
						String.format(
								Locale.ROOT,
								"Could not determine target type for dynamic attribute [%s, %s]",
								declaringType,
								jaxbPluralAttribute.getName()
						)
				);
			}
		}

		return TypeDetails.parameterizedType( collectionClassDetails, typeParams, declaringType );
	}

	private static ClassDetails collectionType(MutableClassDetailsRegistry classDetailsRegistry) {
		if ( COLLECTION_CLASS_DETAILS == null ) {
			COLLECTION_CLASS_DETAILS = classDetailsRegistry.getClassDetails( Collection.class.getName() );
		}
		return COLLECTION_CLASS_DETAILS;
	}

	private static ClassDetails listType(MutableClassDetailsRegistry classDetailsRegistry) {
		if ( LIST_CLASS_DETAILS == null ) {
			LIST_CLASS_DETAILS = classDetailsRegistry.getClassDetails( List.class.getName() );
		}
		return LIST_CLASS_DETAILS;
	}

	private static ClassDetails setType(JaxbPluralAttribute jaxbPluralAttribute, MutableClassDetailsRegistry classDetailsRegistry) {
		if ( isSorted( jaxbPluralAttribute ) ) {
			if ( SORTED_SET_CLASS_DETAILS == null ) {
				SORTED_SET_CLASS_DETAILS = classDetailsRegistry.getClassDetails( SortedSet.class.getName() );
			}
			return SORTED_SET_CLASS_DETAILS;
		}
		else {
			if ( SET_CLASS_DETAILS == null ) {
				SET_CLASS_DETAILS = classDetailsRegistry.getClassDetails( Set.class.getName() );
			}
			return SET_CLASS_DETAILS;
		}
	}

	private static boolean isSorted(JaxbPluralAttribute jaxbPluralAttribute) {
		return isNotEmpty( jaxbPluralAttribute.getSort() )
			|| jaxbPluralAttribute.getSortNatural() != null
			|| isNotEmpty( jaxbPluralAttribute.getOrderBy() );
	}

	private static ClassDetails mapType(JaxbPluralAttribute jaxbPluralAttribute, MutableClassDetailsRegistry classDetailsRegistry) {
		if ( isSorted( jaxbPluralAttribute ) ) {
			if ( SORTED_MAP_CLASS_DETAILS == null ) {
				SORTED_MAP_CLASS_DETAILS = classDetailsRegistry.getClassDetails( SortedMap.class.getName() );
			}
			return SORTED_MAP_CLASS_DETAILS;
		}
		else {
			if ( MAP_CLASS_DETAILS == null ) {
				MAP_CLASS_DETAILS = classDetailsRegistry.getClassDetails( Map.class.getName() );
			}
			return MAP_CLASS_DETAILS;
		}
	}

	/**
	 * Determine the appropriate "Java type" for the given basic mapping.
	 * Wraps the result of {@linkplain #determineAttributeJavaType} in a {@linkplain ClassTypeDetailsImpl}
	 * Handles {@code <id/>}, {@code <basic/>}, {@code <tenant-id/>}.
	 */
	private static TypeDetails determineAttributeJavaTypeDetails(
			JaxbManagedType declaringType,
			JaxbBasicMapping jaxbBasicMapping,
			XmlDocumentContext xmlDocumentContext) {
		return TypeDetails.classType( determineAttributeJavaType( declaringType, jaxbBasicMapping, xmlDocumentContext ) );
	}

	private static ClassDetails determineAttributeJavaType(
			JaxbManagedType declaringType,
			JaxbBasicMapping jaxbBasicMapping,
			XmlDocumentContext xmlDocumentContext) {
		// explicit <target/>
		final String target = jaxbBasicMapping.getTarget();
		if ( isNotEmpty( target ) ) {
			final var simpleTypeInterpretation = SimpleTypeInterpretation.interpret( target );
			if ( simpleTypeInterpretation == null ) {
				throw new UnknownAttributeTypeException(
						String.format(
								Locale.ROOT,
								"Could not determine target type for dynamic attribute [%s, %s]",
								declaringType,
								jaxbBasicMapping.getName()
						)
				);
			}
			return resolveBasicMappingTarget( simpleTypeInterpretation, xmlDocumentContext );
		}

		final ModelsContext modelsContext = xmlDocumentContext.getModelsContext();

		// UserType
		final var userTypeNode = jaxbBasicMapping.getType();
		if ( userTypeNode != null ) {
			final String userTypeImplName = userTypeNode.getValue();
			if ( isNotEmpty( userTypeImplName ) ) {
				final ClassDetails userTypeImplDetails = xmlDocumentContext.resolveJavaType( userTypeImplName );
				// safe to convert to class, though unfortunate to have to instantiate it...
				final UserType<?> userType = createInstance( userTypeImplDetails );
				final Class<?> modelClass = userType.returnedClass();
				return modelsContext.getClassDetailsRegistry().getClassDetails( modelClass.getName() );
			}
		}

		// JavaType
		final String javaTypeImplName = jaxbBasicMapping.getJavaType();
		if ( isNotEmpty( javaTypeImplName ) ) {
			final var javaTypeImplDetails = xmlDocumentContext.resolveJavaType( javaTypeImplName );
			// safe to convert to class, though unfortunate to have to instantiate it...
			final JavaType<?> javaType = createInstance( javaTypeImplDetails );
			final var modelClass = javaType.getJavaTypeClass();
			return modelsContext.getClassDetailsRegistry().getClassDetails( modelClass.getName() );
		}

		// JdbcType
		final String jdbcTypeImplName = jaxbBasicMapping.getJdbcType();
		final Integer jdbcTypeCode = jaxbBasicMapping.getJdbcTypeCode();
		final JdbcType jdbcType;
		if ( isNotEmpty( jdbcTypeImplName ) ) {
			final var jdbcTypeImplDetails = xmlDocumentContext.resolveJavaType( javaTypeImplName );
			jdbcType = createInstance( jdbcTypeImplDetails );
		}
		else if ( jdbcTypeCode != null ) {
			jdbcType = xmlDocumentContext.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( jdbcTypeCode );
		}
		else {
			jdbcType = null;
		}
		if ( jdbcType != null ) {
			final var javaType = jdbcType.getRecommendedJavaType( 0, 0, xmlDocumentContext.getTypeConfiguration() );
			final var modelClass = javaType.getJavaTypeClass();
			return modelsContext.getClassDetailsRegistry().getClassDetails( modelClass.getName() );
		}

		if ( jaxbBasicMapping instanceof JaxbBasicImpl jaxbBasicAttribute ) {
			final var temporalType = jaxbBasicAttribute.getTemporal();
			if ( temporalType != null ) {
				return resolveTemporalJavaType( temporalType, xmlDocumentContext );
			}
		}

		final String declaringTypeName;
		if ( declaringType instanceof JaxbEntity jaxbEntity ) {
			declaringTypeName = nullIfEmpty( jaxbEntity.getName() );
		}
		else if ( declaringType instanceof JaxbEmbeddable jaxbEmbeddable ) {
			declaringTypeName = nullIfEmpty( jaxbEmbeddable.getName() );
		}
		else {
			declaringTypeName = null;
		}

		throw new UnknownAttributeTypeException(
				String.format(
						Locale.ROOT,
						"Could not determine target type for dynamic attribute [%s#%s]",
						declaringTypeName != null ? declaringTypeName : declaringType.getClazz(),
						jaxbBasicMapping.getName()
				)
		);
	}

	/**
	 * Conceptually very similar to {@linkplain SimpleTypeInterpretation}, but here the distinction between
	 * primitive and wrapper ({@code boolean} and {@code Boolean} e.g.) is important while in
	 * SimpleTypeInterpretation we only care about the wrapper.
	 */
	private static ClassDetails resolveBasicMappingTarget(SimpleTypeInterpretation targetInterpretation, XmlDocumentContext xmlDocumentContext) {
		return xmlDocumentContext.getClassDetailsRegistry().resolveClassDetails( targetInterpretation.getJavaType().getName() );
	}

	private static MutableClassDetails resolveTemporalJavaType(
			TemporalType temporalType,
			XmlDocumentContext xmlDocumentContext) {
		final var modelsContext = xmlDocumentContext.getModelsContext();
		final var classDetailsRegistry = xmlDocumentContext.getClassDetailsRegistry().as( MutableClassDetailsRegistry.class );
		switch ( temporalType ) {
			case DATE -> {
				return (MutableClassDetails) classDetailsRegistry.resolveClassDetails(
						java.sql.Date.class.getName(),
						name -> Creator.createJdkClassDetails( java.sql.Date.class, modelsContext )
				);
			}
			case TIME -> {
				return (MutableClassDetails) classDetailsRegistry.resolveClassDetails(
						java.sql.Time.class.getName(),
						name -> Creator.createJdkClassDetails( java.sql.Time.class, modelsContext )
				);
			}
			default -> {
				return (MutableClassDetails) classDetailsRegistry.resolveClassDetails(
						java.sql.Timestamp.class.getName(),
						name -> Creator.createJdkClassDetails( java.sql.Timestamp.class, modelsContext )
				);
			}
		}
	}

	/**
	 * Determine the appropriate TypeDetails for the given embedded mapping.
	 * Handles {@code <embedded/>}, {@code <embedded-id/>}
	 */
	private static TypeDetails determineAttributeJavaTypeDetails(
			JaxbManagedType declaringType,
			JaxbEmbeddedMapping jaxbEmbeddedMapping,
			XmlDocumentContext xmlDocumentContext) {
		final String target = jaxbEmbeddedMapping.getTarget();
		if ( isNotEmpty( target ) ) {
			final ModelsContext modelsContext = xmlDocumentContext.getModelsContext();
			final ClassDetails memberTypeClassDetails = ModelsHelper.resolveClassDetails(
					target,
					xmlDocumentContext,
					modelsContext.getClassDetailsRegistry(),
					() -> Creator.createDynamicClassDetails( target, modelsContext )
			);

			return TypeDetails.classType( memberTypeClassDetails );
		}

		// todo : need more context here for better exception message
		throw new HibernateException( "Could not determine target type for dynamic attribute" );
	}

	/**
	 * Determine the appropriate TypeDetails for the given association mapping.
	 * Handles {@code <one-to-one/>}, {@code <many-to-one/>}, {@code <one-to-many/>}, {@code <many-to-many/>}
	 */
	private static TypeDetails determineAttributeJavaTypeDetails(
			JaxbAssociationAttribute jaxbAssociationAttribute,
			XmlDocumentContext xmlDocumentContext) {
		final String target = jaxbAssociationAttribute.getTargetEntity();
		if ( isNotEmpty( target ) ) {
			final var modelsContext = xmlDocumentContext.getModelsContext();
			final var classDetailsRegistry = xmlDocumentContext.getClassDetailsRegistry();
			final String targetEntityName = xmlDocumentContext.resolveTargetEntityName( target );
			final var existingClassDetails = classDetailsRegistry.findClassDetails( targetEntityName );
			if ( existingClassDetails != null ) {
				return TypeDetails.classType( existingClassDetails );
			}
			if ( xmlDocumentContext.isDynamicManagedTypeName( targetEntityName ) ) {
				final var dynamicClassDetails = ModelsHelper.resolveClassDetails(
						targetEntityName,
						classDetailsRegistry,
						() -> Creator.createDynamicClassDetails( targetEntityName, modelsContext )
				);
				return TypeDetails.classType( dynamicClassDetails );
			}
			final var classDetails = ModelsHelper.resolveClassDetails(
					targetEntityName,
					xmlDocumentContext,
					classDetailsRegistry,
					() -> Creator.createDynamicClassDetails( targetEntityName, modelsContext )
			);
			return TypeDetails.classType( classDetails );
		}

		// todo : need more context here for better exception message
		throw new HibernateException( "Could not determine target type for dynamic attribute" );
	}

	/**
	 * Determine the appropriate TypeDetails for the given ANY mapping.
	 * Handles {@code <any/>}, {@code <many-to-any/>}.
	 */
	private static TypeDetails determineAttributeJavaTypeDetails(
			JaxbAnyMapping jaxbAnyMapping,
			XmlDocumentContext xmlDocumentContext) {
		// Logically this is Object, which is what we return here for now.
		// todo : might be nice to allow specifying a "common interface"
		final var objectClassDetails = xmlDocumentContext.getClassDetailsRegistry().resolveClassDetails( OBJECT_CLASS_NAME );
		return TypeDetails.classType( objectClassDetails );
	}

	/**
	 * Determine the appropriate TypeDetails for the given ANY mapping.
	 * Handles {@code <element-collection/>}.
	 */
	private static TypeDetails determineAttributeJavaTypeDetails(
			JaxbElementCollectionImpl jaxbElementCollection,
			XmlDocumentContext xmlDocumentContext) {
		final var classification = nullif( jaxbElementCollection.getClassification(), LimitedCollectionClassification.BAG );
		return switch ( classification ) {
			case BAG -> resolveCollectionType( Collection.class, xmlDocumentContext );
			case LIST -> resolveCollectionType( List.class, xmlDocumentContext );
			case SET -> resolveCollectionType( Set.class, xmlDocumentContext );
			case MAP -> resolveCollectionType( Map.class, xmlDocumentContext );
		};
	}

	private static TypeDetails resolveCollectionType(Class<?> collectionType, XmlDocumentContext xmlDocumentContext) {
		final var classDetails = xmlDocumentContext
				.getClassDetailsRegistry()
				.resolveClassDetails( collectionType.getName() );
		return TypeDetails.classType( classDetails );
	}

	@Nonnull
	private static <T> T createInstance(ClassDetails classDetails) {
		try {
			//noinspection unchecked
			return (T) classDetails.toJavaClass().getConstructor().newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to create instance from incoming ClassDetails - " + classDetails );
		}
	}
}
