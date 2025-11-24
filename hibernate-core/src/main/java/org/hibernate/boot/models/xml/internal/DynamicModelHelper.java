/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import jakarta.persistence.TemporalType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.HibernateException;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAssociationAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddable;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTenantIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.boot.models.xml.UnknownAttributeTypeException;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.internal.ParameterizedTypeDetailsImpl;
import org.hibernate.models.internal.WildcardTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.internal.jdk.JdkClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableClassDetails;
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
import static org.hibernate.models.internal.ModifierUtils.DYNAMIC_ATTRIBUTE_MODIFIERS;

/**
 * Used from {@linkplain ManagedTypeProcessor} to help dealing with dynamic models
 *
 * @author Steve Ebersole
 */
public class DynamicModelHelper {
	/**
	 * Creates DynamicFieldDetails for each attribute defined in the XML
	 */
	static void prepareDynamicClass(
			MutableClassDetails classDetails,
			JaxbManagedType jaxbManagedType,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbManagedType instanceof JaxbEntityImpl jaxbDynamicEntity ) {
			final JaxbAttributesContainerImpl attributes = jaxbDynamicEntity.getAttributes();

			if ( attributes != null ) {
				if ( CollectionHelper.isNotEmpty( attributes.getIdAttributes() ) ) {
					// <id/>
					attributes.getIdAttributes().forEach( (jaxbId) -> {
						final TypeDetails attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbManagedType,
								jaxbId,
								xmlDocumentContext
						);
						final DynamicFieldDetails member = new DynamicFieldDetails(
								jaxbId.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
								false,
								false,
								xmlDocumentContext.getModelBuildingContext()
						);
						classDetails.addField( member );
					} );
				}
				else if ( attributes.getEmbeddedIdAttribute() != null ) {
					// <embedded-id/>
					final JaxbEmbeddedIdImpl embeddedId = attributes.getEmbeddedIdAttribute();
					final TypeDetails attributeJavaType = determineAttributeJavaTypeDetails(
							jaxbManagedType,
							embeddedId,
							xmlDocumentContext
					);
					final DynamicFieldDetails member = new DynamicFieldDetails(
							embeddedId.getName(),
							attributeJavaType,
							classDetails,
							DYNAMIC_ATTRIBUTE_MODIFIERS,
							false,
							false,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				}

				// <natural-id/>
				if ( attributes.getNaturalId() != null ) {
					attributes.getNaturalId().getBasicAttributes().forEach( (jaxbBasic) -> {
						final TypeDetails attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbManagedType,
								jaxbBasic,
								xmlDocumentContext
						);
						final DynamicFieldDetails member = new DynamicFieldDetails(
								jaxbBasic.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
								false,
								false,
								xmlDocumentContext.getModelBuildingContext()
						);
						classDetails.addField( member );
					} );

					attributes.getNaturalId().getEmbeddedAttributes().forEach( (jaxbEmbedded) -> {
						final TypeDetails attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbManagedType,
								jaxbEmbedded,
								xmlDocumentContext
						);
						final DynamicFieldDetails member = new DynamicFieldDetails(
								jaxbEmbedded.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
								false,
								false,
								xmlDocumentContext.getModelBuildingContext()
						);
						classDetails.addField( member );
					} );

					attributes.getNaturalId().getManyToOneAttributes().forEach( (jaxbManyToOne) -> {
						final TypeDetails attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbManyToOne,
								xmlDocumentContext
						);
						final DynamicFieldDetails member = new DynamicFieldDetails(
								jaxbManyToOne.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
								false,
								false,
								xmlDocumentContext.getModelBuildingContext()
						);
						classDetails.addField( member );
					} );

					attributes.getNaturalId().getAnyMappingAttributes().forEach( (jaxbAnyMapping) -> {
						final TypeDetails attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbAnyMapping,
								xmlDocumentContext
						);
						final DynamicFieldDetails member = new DynamicFieldDetails(
								jaxbAnyMapping.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
								false,
								false,
								xmlDocumentContext.getModelBuildingContext()
						);
						classDetails.addField( member );
					} );
				}
			}

			// <tenant-id>
			final JaxbTenantIdImpl tenantId = jaxbDynamicEntity.getTenantId();
			if ( tenantId != null ) {
				final TypeDetails attributeJavaType = determineAttributeJavaTypeDetails(
						jaxbManagedType,
						tenantId,
						xmlDocumentContext
				);
				final DynamicFieldDetails member = new DynamicFieldDetails(
						tenantId.getName(),
						attributeJavaType,
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						false,
						xmlDocumentContext.getModelBuildingContext()
				);
				classDetails.addField( member );
			}
		}
		else if ( jaxbManagedType instanceof JaxbMappedSuperclassImpl jaxbMappedSuperclass ) {
			final JaxbAttributesContainerImpl attributes = jaxbMappedSuperclass.getAttributes();

			if ( attributes != null ) {
				if ( CollectionHelper.isNotEmpty( attributes.getIdAttributes() ) ) {
					// <id/>
					attributes.getIdAttributes().forEach( (jaxbId) -> {
						final TypeDetails attributeJavaType = determineAttributeJavaTypeDetails(
								jaxbManagedType,
								jaxbId,
								xmlDocumentContext
						);
						final DynamicFieldDetails member = new DynamicFieldDetails(
								jaxbId.getName(),
								attributeJavaType,
								classDetails,
								DYNAMIC_ATTRIBUTE_MODIFIERS,
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
					final TypeDetails attributeJavaType = determineAttributeJavaTypeDetails(
							jaxbManagedType,
							embeddedId,
							xmlDocumentContext
					);
					final DynamicFieldDetails member = new DynamicFieldDetails(
							embeddedId.getName(),
							attributeJavaType,
							classDetails,
							DYNAMIC_ATTRIBUTE_MODIFIERS,
							false,
							false,
							xmlDocumentContext.getModelBuildingContext()
					);
					classDetails.addField( member );
				}
			}
		}

		final JaxbAttributesContainer attributes = jaxbManagedType.getAttributes();

		if ( attributes != null ) {
			// <basic/>
			attributes.getBasicAttributes().forEach( (jaxbBasic) -> {
				final DynamicFieldDetails member = new DynamicFieldDetails(
						jaxbBasic.getName(),
						determineAttributeJavaTypeDetails( jaxbManagedType, jaxbBasic, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
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
						determineAttributeJavaTypeDetails( jaxbManagedType, jaxbEmbedded, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
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
						determineAttributeJavaTypeDetails( jaxbOneToOne, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
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
						determineAttributeJavaTypeDetails( jaxbManyToOne, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
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
						determineAttributeJavaTypeDetails( jaxbAnyMapping, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						false,
						xmlDocumentContext.getModelBuildingContext()
				);
				classDetails.addField( member );
			} );

			// <element-collection/>
			attributes.getElementCollectionAttributes().forEach( (jaxbElementCollection) -> {
				final TypeDetails elementType = determineAttributeJavaTypeDetails( jaxbElementCollection, xmlDocumentContext );
				final DynamicFieldDetails member = new DynamicFieldDetails(
						jaxbElementCollection.getName(),
						makeCollectionType( classDetails, jaxbElementCollection, elementType, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						true,
						xmlDocumentContext.getModelBuildingContext()
				);
				classDetails.addField( member );
			} );

			// <one-to-many/>
			attributes.getOneToManyAttributes().forEach( (jaxbOneToMany) -> {
				final TypeDetails elementType = determineAttributeJavaTypeDetails( jaxbOneToMany, xmlDocumentContext );
				final DynamicFieldDetails member = new DynamicFieldDetails(
						jaxbOneToMany.getName(),
						// todo : this is wrong.  should be the collection-type (List, ...)
						//  	wrapping the result from determineAttributeJavaTypeDetails
						makeCollectionType( classDetails, jaxbOneToMany, elementType, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						true,
						xmlDocumentContext.getModelBuildingContext()
				);
				classDetails.addField( member );
			} );

			// <many-to-many/>
			attributes.getManyToManyAttributes().forEach( (jaxbManyToMany) -> {
				final TypeDetails elementType = determineAttributeJavaTypeDetails( jaxbManyToMany, xmlDocumentContext );
				final DynamicFieldDetails member = new DynamicFieldDetails(
						jaxbManyToMany.getName(),
						makeCollectionType( classDetails, jaxbManyToMany, elementType, xmlDocumentContext ),
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						true,
						xmlDocumentContext.getModelBuildingContext()
				);
				classDetails.addField( member );
			} );

			// <many-to-any/>
			attributes.getPluralAnyMappingAttributes().forEach( (jaxbPluralAnyMapping) -> {
				final TypeDetails attributeType = determineAttributeJavaTypeDetails(
						jaxbPluralAnyMapping,
						xmlDocumentContext
				);
				final DynamicFieldDetails member = new DynamicFieldDetails(
						jaxbPluralAnyMapping.getName(),
						attributeType,
						classDetails,
						DYNAMIC_ATTRIBUTE_MODIFIERS,
						false,
						true,
						xmlDocumentContext.getModelBuildingContext()
				);
				classDetails.addField( member );
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
		final MutableClassDetailsRegistry classDetailsRegistry = xmlDocumentContext
				.getBootstrapContext()
				.getModelsContext()
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
						new WildcardTypeDetailsImpl( new ClassTypeDetailsImpl( objectClassDetails, TypeDetails.Kind.CLASS ), true ),
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

		return new ParameterizedTypeDetailsImpl( collectionClassDetails, typeParams, declaringType );
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
		return StringHelper.isNotEmpty( jaxbPluralAttribute.getSort() )
			|| jaxbPluralAttribute.getSortNatural() != null
			|| StringHelper.isNotEmpty( jaxbPluralAttribute.getOrderBy() );
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
		return new ClassTypeDetailsImpl( determineAttributeJavaType( declaringType, jaxbBasicMapping, xmlDocumentContext ), TypeDetails.Kind.CLASS );
	}

	private static ClassDetails determineAttributeJavaType(
			JaxbManagedType declaringType,
			JaxbBasicMapping jaxbBasicMapping,
			XmlDocumentContext xmlDocumentContext) {
		// explicit <target/>
		final String target = jaxbBasicMapping.getTarget();
		if ( isNotEmpty( target ) ) {
			final SimpleTypeInterpretation simpleTypeInterpretation = SimpleTypeInterpretation.interpret( target );
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

		final BootstrapContext bootstrapContext = xmlDocumentContext.getBootstrapContext();
		final ModelsContext modelsContext = bootstrapContext.getModelsContext();

		// UserType
		final JaxbUserTypeImpl userTypeNode = jaxbBasicMapping.getType();
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
			final ClassDetails javaTypeImplDetails = xmlDocumentContext.resolveJavaType( javaTypeImplName );
			// safe to convert to class, though unfortunate to have to instantiate it...
			final JavaType<?> javaType = createInstance( javaTypeImplDetails );
			final Class<?> modelClass = javaType.getJavaTypeClass();
			return modelsContext.getClassDetailsRegistry().getClassDetails( modelClass.getName() );
		}

		// JdbcType
		final String jdbcTypeImplName = jaxbBasicMapping.getJdbcType();
		final Integer jdbcTypeCode = jaxbBasicMapping.getJdbcTypeCode();
		final JdbcType jdbcType;
		if ( isNotEmpty( jdbcTypeImplName ) ) {
			final ClassDetails jdbcTypeImplDetails = xmlDocumentContext.resolveJavaType( javaTypeImplName );
			jdbcType = createInstance( jdbcTypeImplDetails );
		}
		else if ( jdbcTypeCode != null ) {
			jdbcType = bootstrapContext.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( jdbcTypeCode );
		}
		else {
			jdbcType = null;
		}
		if ( jdbcType != null ) {
			final JavaType<?> javaType = jdbcType.getJdbcRecommendedJavaTypeMapping( 0, 0, bootstrapContext.getTypeConfiguration() );
			final Class<?> modelClass = javaType.getJavaTypeClass();
			return modelsContext.getClassDetailsRegistry().getClassDetails( modelClass.getName() );
		}

		if ( jaxbBasicMapping instanceof JaxbBasicImpl jaxbBasicAttribute ) {
			final TemporalType temporalType = jaxbBasicAttribute.getTemporal();
			if ( temporalType != null ) {
				return resolveTemporalJavaType( temporalType, xmlDocumentContext );
			}
		}

		final String declaringTypeName;
		if ( declaringType instanceof JaxbEntity jaxbEntity ) {
			declaringTypeName = StringHelper.nullIfEmpty( jaxbEntity.getName() );
		}
		else if ( declaringType instanceof JaxbEmbeddable jaxbEmbeddable ) {
			declaringTypeName = StringHelper.nullIfEmpty( jaxbEmbeddable.getName() );
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
		final ModelsContext modelsContext = xmlDocumentContext.getBootstrapContext().getModelsContext();
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();
		return classDetailsRegistry.resolveClassDetails( targetInterpretation.getJavaType().getName() );
	}

	private static MutableClassDetails resolveTemporalJavaType(
			TemporalType temporalType,
			XmlDocumentContext xmlDocumentContext) {
		final ModelsContext modelsContext = xmlDocumentContext.getBootstrapContext().getModelsContext();
		final MutableClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry().as( MutableClassDetailsRegistry.class );
		switch ( temporalType ) {
			case DATE -> {
				return (MutableClassDetails) classDetailsRegistry.resolveClassDetails(
						java.sql.Date.class.getName(),
						name -> new JdkClassDetails( java.sql.Date.class, modelsContext )
				);
			}
			case TIME -> {
				return (MutableClassDetails) classDetailsRegistry.resolveClassDetails(
						java.sql.Time.class.getName(),
						name -> new JdkClassDetails( java.sql.Time.class, modelsContext )
				);
			}
			default -> {
				return (MutableClassDetails) classDetailsRegistry.resolveClassDetails(
						java.sql.Timestamp.class.getName(),
						name -> new JdkClassDetails( java.sql.Timestamp.class, modelsContext )
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
			final ModelsContext modelsContext = xmlDocumentContext.getBootstrapContext().getModelsContext();
			final ClassDetails memberTypeClassDetails = ModelsHelper.resolveClassDetails(
					target,
					modelsContext.getClassDetailsRegistry(),
					() -> new DynamicClassDetails( target, modelsContext )
			);

			return new ClassTypeDetailsImpl( memberTypeClassDetails, TypeDetails.Kind.CLASS );
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
			final ModelsContext modelsContext = xmlDocumentContext.getBootstrapContext().getModelsContext();
			final ClassDetails classDetails = ModelsHelper.resolveClassDetails(
					target,
					modelsContext.getClassDetailsRegistry(),
					() -> new DynamicClassDetails(
							target,
							null,
							false,
							null,
							null,
							modelsContext
					)
			);
			return new ClassTypeDetailsImpl( classDetails, TypeDetails.Kind.CLASS );
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
		final ModelsContext modelsContext = xmlDocumentContext.getBootstrapContext().getModelsContext();
		final ClassDetails objectClassDetails = modelsContext.getClassDetailsRegistry().resolveClassDetails( OBJECT_CLASS_NAME );
		return new ClassTypeDetailsImpl( objectClassDetails, TypeDetails.Kind.CLASS );
	}

	/**
	 * Determine the appropriate TypeDetails for the given ANY mapping.
	 * Handles {@code <element-collection/>}.
	 */
	private static TypeDetails determineAttributeJavaTypeDetails(
			JaxbElementCollectionImpl jaxbElementCollection,
			XmlDocumentContext xmlDocumentContext) {
		final LimitedCollectionClassification classification = nullif( jaxbElementCollection.getClassification(), LimitedCollectionClassification.BAG );
		return switch ( classification ) {
			case BAG -> resolveCollectionType( Collection.class, xmlDocumentContext );
			case LIST -> resolveCollectionType( List.class, xmlDocumentContext );
			case SET -> resolveCollectionType( Set.class, xmlDocumentContext );
			case MAP -> resolveCollectionType( Map.class, xmlDocumentContext );
		};
	}

	private static TypeDetails resolveCollectionType(Class<?> collectionType, XmlDocumentContext xmlDocumentContext) {
		final ClassDetails classDetails = xmlDocumentContext
				.getBootstrapContext()
				.getModelsContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( collectionType.getName() );
		return new ClassTypeDetailsImpl( classDetails, TypeDetails.Kind.CLASS );
	}

	@NonNull
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
