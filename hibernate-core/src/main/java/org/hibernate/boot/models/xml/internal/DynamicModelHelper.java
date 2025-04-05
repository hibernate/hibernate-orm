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
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTenantIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.internal.jdk.JdkBuilders;
import org.hibernate.models.internal.jdk.JdkClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.UserType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hibernate.internal.util.NullnessHelper.nullif;
import static org.hibernate.internal.util.StringHelper.isEmpty;
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
						determineAttributeJavaTypeDetails( jaxbBasic, xmlDocumentContext ),
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
						determineAttributeJavaTypeDetails( jaxbEmbedded, xmlDocumentContext ),
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
				final DynamicFieldDetails member = new DynamicFieldDetails(
						jaxbElementCollection.getName(),
						determineAttributeJavaTypeDetails( jaxbElementCollection, xmlDocumentContext ),
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
				final DynamicFieldDetails member = new DynamicFieldDetails(
						jaxbOneToMany.getName(),
						determineAttributeJavaTypeDetails( jaxbOneToMany, xmlDocumentContext ),
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
				final DynamicFieldDetails member = new DynamicFieldDetails(
						jaxbManyToMany.getName(),
						determineAttributeJavaTypeDetails( jaxbManyToMany, xmlDocumentContext ),
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

	/**
	 * Determine the appropriate "Java type" for the given basic mapping.
	 * Wraps the result of {@linkplain #determineAttributeJavaType} in a {@linkplain ClassTypeDetailsImpl}
	 * Handles {@code <id/>}, {@code <basic/>}, {@code <tenant-id/>}.
	 */
	private static TypeDetails determineAttributeJavaTypeDetails(
			JaxbBasicMapping jaxbBasicMapping,
			XmlDocumentContext xmlDocumentContext) {
		return new ClassTypeDetailsImpl( determineAttributeJavaType( jaxbBasicMapping, xmlDocumentContext ), TypeDetails.Kind.CLASS );
	}

	private static ClassDetails determineAttributeJavaType(
			JaxbBasicMapping jaxbBasicMapping,
			XmlDocumentContext xmlDocumentContext) {
		// explicit <target/>
		final String target = jaxbBasicMapping.getTarget();
		if ( isNotEmpty( target ) ) {
			final SimpleTypeInterpretation simpleTypeInterpretation = SimpleTypeInterpretation.interpret( target );
			return resolveBasicMappingTarget( target, xmlDocumentContext );
		}

		final BootstrapContext bootstrapContext = xmlDocumentContext.getBootstrapContext();
		final SourceModelBuildingContext modelsContext = bootstrapContext.getModelsContext();

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

		// todo : need more context here for better exception message
		throw new HibernateException( "Could not determine target type for dynamic attribute" );
	}

	/**
	 * Conceptually very similar to {@linkplain SimpleTypeInterpretation}, but here the distinction between
	 * primitive and wrapper ({@code boolean} and {@code Boolean} e.g.) is important while in
	 * SimpleTypeInterpretation we only care about the wrapper.
	 */
	private static ClassDetails resolveBasicMappingTarget(String name, XmlDocumentContext xmlDocumentContext) {
		final SourceModelBuildingContext modelsContext = xmlDocumentContext.getBootstrapContext().getModelsContext();
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		if ( isEmpty( name ) ) {
			return classDetailsRegistry.resolveClassDetails( Object.class.getName() );
		}

		final SimpleTypeInterpretation simpleTypeInterpretation = SimpleTypeInterpretation.interpret( name );
		if ( simpleTypeInterpretation != null ) {
			return classDetailsRegistry.resolveClassDetails( simpleTypeInterpretation.getJavaType().getName() );
		}

		name = StringHelper.qualifyConditionallyIfNot( xmlDocumentContext.getXmlDocument().getDefaults().getPackage(), name );
		return classDetailsRegistry.as( MutableClassDetailsRegistry.class ).resolveClassDetails(
				name,
				(s) -> JdkBuilders.buildClassDetailsStatic( s,  modelsContext )
		);
	}

	private static MutableClassDetails resolveTemporalJavaType(
			TemporalType temporalType,
			XmlDocumentContext xmlDocumentContext) {
		final SourceModelBuildingContext modelsContext = xmlDocumentContext.getBootstrapContext().getModelsContext();
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
			JaxbEmbeddedMapping jaxbEmbeddedMapping,
			XmlDocumentContext xmlDocumentContext) {
		final String target = jaxbEmbeddedMapping.getTarget();
		if ( isNotEmpty( target ) ) {
			final SourceModelBuildingContext modelsContext = xmlDocumentContext.getBootstrapContext().getModelsContext();
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
			final SourceModelBuildingContext modelsContext = xmlDocumentContext.getBootstrapContext().getModelsContext();
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
		final SourceModelBuildingContext modelsContext = xmlDocumentContext.getBootstrapContext().getModelsContext();
		final ClassDetails objectClassDetails = modelsContext.getClassDetailsRegistry().resolveClassDetails( Object.class.getName() );
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
