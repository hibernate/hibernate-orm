/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AccessType;
import jakarta.persistence.Access;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.TargetEmbeddable;
import org.hibernate.boot.model.IdentifierGeneratorRegistration;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.boot.mapping.spi.ValueMetadata;
import org.hibernate.boot.mapping.spi.ValueNature;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import static org.hibernate.boot.mapping.internal.categorize.CategorizationHelper.determineAttributeNature;

/// Creates categorized attribute metadata, including usage-specific embedded
/// structure.
///
/// @since 9.0
/// @author Steve Ebersole
final class AttributeMetadataResolver {
	private AttributeMetadataResolver() {
	}

	static AttributeMetadataImplementor resolve(
			MemberDetails member,
			TypeDetails memberType,
			AccessType inheritedAccessType,
			CategorizationContext context) {
		return resolve( member, memberType, inheritedAccessType, context, new java.util.LinkedHashSet<>() );
	}

	static AttributeMetadataImplementor resolve(
			MemberDetails member,
			TypeDetails memberType,
			AccessType inheritedAccessType,
			CategorizationContext context,
			Set<String> embeddableResolutionPath) {
		final Access access = member.getDirectAnnotationUsage( Access.class );
		final AccessType effectiveAccessType = access == null ? inheritedAccessType : access.value();
		final AttributeNature nature = isRegisteredCompositeUserType( memberType, context )
				? AttributeNature.EMBEDDED
				: determineAttributeNature( member, memberType );
		if ( isPlural( nature ) ) {
			return pluralAttribute(
					member,
					memberType,
					nature,
					effectiveAccessType,
					context,
					embeddableResolutionPath
			);
		}
		if ( nature != AttributeNature.EMBEDDED ) {
			return new SingularAttributeMetadataImpl(
					member.resolveAttributeName(),
					member,
					new ValueMetadataImpl( memberType, valueNature( nature ) )
			);
		}

		final TargetEmbeddable targetEmbeddable = member.getDirectAnnotationUsage( TargetEmbeddable.class );
		final ClassDetails embeddableClass = targetEmbeddable == null
				? memberType.determineRawClass()
				: context.getClassDetailsRegistry().resolveClassDetails( targetEmbeddable.value().getName() );
		final EmbeddableTypeMetadataImpl embeddableType = context.findEmbeddableType( embeddableClass );
		if ( embeddableType == null ) {
			return new SingularAttributeMetadataImpl(
					member.resolveAttributeName(),
					member,
					new ValueMetadataImpl(
							targetEmbeddable == null
									? memberType
									: TypeDetails.classType( embeddableClass ),
							ValueNature.EMBEDDED
					)
			);
		}
		final EmbeddableUsageMetadataImpl usage = ( (EmbeddableTypeMetadataImpl) embeddableType ).resolveUsage(
				member,
				targetEmbeddable == null ? memberType : TypeDetails.classType( embeddableClass ),
				effectiveAccessType,
				context,
				embeddableResolutionPath
		);
		final TypeDetails embeddedType = targetEmbeddable == null
				? memberType
				: TypeDetails.classType( embeddableClass );
		return new EmbeddedAttributeMetadataImpl(
				member.resolveAttributeName(),
				member,
				embeddedValue(
						embeddedType,
						usage,
						member,
						effectiveAccessType,
						context,
						embeddableResolutionPath
				)
		);
	}

	private static boolean isRegisteredCompositeUserType(
			TypeDetails memberType,
			CategorizationContext context) {
		final String memberTypeName = memberType.determineRawClass().getName();
		for ( CompositeUserTypeRegistration registration
				: context.getGlobalRegistrations().getCompositeUserTypeRegistrations() ) {
			if ( registration.embeddableClass().getName().equals( memberTypeName ) ) {
				return true;
			}
		}
		return false;
	}

	private static PluralAttributeMetadataImpl pluralAttribute(
			MemberDetails member,
			TypeDetails collectionType,
			AttributeNature nature,
			AccessType inheritedAccessType,
			CategorizationContext context,
			Set<String> embeddableResolutionPath) {
		final CollectionSource source = collectionSource( member, collectionType, nature, context );
		return new PluralAttributeMetadataImpl(
				member.resolveAttributeName(),
				nature,
				member,
				collectionType,
				source.classification(),
				value(
						source.elementType(),
						elementNature( source ),
						member,
						inheritedAccessType,
						context,
						embeddableResolutionPath
				),
				indexPart(
						source,
						member,
						inheritedAccessType,
						context,
						embeddableResolutionPath
				),
				collectionId( source, context )
		);
	}

	private static CollectionSource collectionSource(
			MemberDetails member,
			TypeDetails collectionType,
			AttributeNature nature,
			CategorizationContext context) {
		return switch ( nature ) {
			case ELEMENT_COLLECTION -> CollectionSource.elementCollection(
					member,
					collectionType,
					member.getDeclaringType(),
					null,
					context.getModelsContext()
			);
			case MANY_TO_MANY -> CollectionSource.manyToMany(
					member,
					collectionType,
					member.getDeclaringType(),
					null,
					null,
					context.getModelsContext()
			);
			case ONE_TO_MANY -> CollectionSource.oneToMany(
					member,
					collectionType,
					member.getDeclaringType(),
					null,
					null,
					context.getModelsContext()
			);
			case MANY_TO_ANY -> CollectionSource.manyToAny( member, context.getModelsContext() );
			default -> throw new IllegalArgumentException( "Not a plural attribute nature: " + nature );
		};
	}

	private static AttributeNature elementNature(CollectionSource source) {
		return switch ( source.nature() ) {
			case MANY_TO_MANY, ONE_TO_MANY -> AttributeNature.TO_ONE;
			case MANY_TO_ANY -> AttributeNature.ANY;
			case ELEMENT_COLLECTION -> source.hasEmbeddableElement()
							? AttributeNature.EMBEDDED
							: AttributeNature.BASIC;
		};
	}

	private static ValueMetadata indexPart(
			CollectionSource source,
			MemberDetails member,
			AccessType inheritedAccessType,
			CategorizationContext context,
			Set<String> embeddableResolutionPath) {
		if ( source.mapKeyType() != null ) {
			final ClassDetails keyClass = source.mapKeyType().determineRawClass();
			final ValueNature keyNature = keyClass.hasDirectAnnotationUsage( Embeddable.class )
					? ValueNature.EMBEDDED
					: !source.mapKeyJoinColumns().isEmpty()
							|| keyClass.hasDirectAnnotationUsage( Entity.class )
							? ValueNature.TO_ONE
							: ValueNature.BASIC;
			return value(
					source.mapKeyType(),
					keyNature,
					member,
					inheritedAccessType,
					context,
					embeddableResolutionPath
			);
		}
		if ( source.classification() == CollectionClassification.LIST
				|| source.classification() == CollectionClassification.ARRAY ) {
			return new ValueMetadataImpl(
					TypeDetails.classType(
							context.getClassDetailsRegistry().resolveClassDetails( Integer.class.getName() )
					),
					ValueNature.BASIC
			);
		}
		return null;
	}

	private static ValueMetadata value(
			TypeDetails type,
			AttributeNature attributeNature,
			MemberDetails sourceMember,
			AccessType inheritedAccessType,
			CategorizationContext context,
			Set<String> embeddableResolutionPath) {
		return value(
				type,
				valueNature( attributeNature ),
				sourceMember,
				inheritedAccessType,
				context,
				embeddableResolutionPath
		);
	}

	private static ValueMetadata value(
			TypeDetails type,
			ValueNature nature,
			MemberDetails sourceMember,
			AccessType inheritedAccessType,
			CategorizationContext context,
			Set<String> embeddableResolutionPath) {
		final ValueNature effectiveNature = isRegisteredCompositeUserType( type, context )
				? ValueNature.EMBEDDED
				: nature;
		if ( effectiveNature != ValueNature.EMBEDDED ) {
			return new ValueMetadataImpl( type, effectiveNature );
		}
		final EmbeddableTypeMetadataImpl embeddableType = context.findEmbeddableType( type.determineRawClass() );
		if ( embeddableType == null ) {
			return new ValueMetadataImpl( type, ValueNature.EMBEDDED );
		}
		final EmbeddableUsageMetadataImpl usage = ( (EmbeddableTypeMetadataImpl) embeddableType ).resolveUsage(
				sourceMember,
				type,
				inheritedAccessType,
				context,
				embeddableResolutionPath
		);
		return embeddedValue(
				type,
				usage,
				sourceMember,
				inheritedAccessType,
				context,
				embeddableResolutionPath
		);
	}

	private static EmbeddedValueMetadataImpl embeddedValue(
			TypeDetails type,
			EmbeddableUsageMetadataImpl usage,
			MemberDetails sourceMember,
			AccessType inheritedAccessType,
			CategorizationContext context,
			Set<String> embeddableResolutionPath) {
		final ClassDetails baseType = type.determineRawClass();
		final List<EmbeddableUsageMetadataImpl> subtypeUsages = new ArrayList<>();
		for ( EmbeddableTypeMetadataImpl candidate : context.getEmbeddableTypes().values() ) {
			final ClassDetails candidateType = candidate.getClassDetails();
			if ( isStrictSubtype( candidateType, baseType ) ) {
				subtypeUsages.add(
						( (EmbeddableTypeMetadataImpl) candidate ).resolveUsage(
								sourceMember,
								candidateType,
								inheritedAccessType,
								context,
								embeddableResolutionPath
						)
				);
			}
		}
		return new EmbeddedValueMetadataImpl( type, usage, subtypeUsages );
	}

	private static boolean isStrictSubtype(ClassDetails candidate, ClassDetails baseType) {
		if ( candidate.getName().equals( baseType.getName() ) ) {
			return false;
		}
		if ( candidate.isRealClass() && baseType.isRealClass() ) {
			return baseType.toJavaClass().isAssignableFrom( candidate.toJavaClass() );
		}
		for ( ClassDetails current = candidate.getSuperClass();
				current != null && current != ClassDetails.OBJECT_CLASS_DETAILS;
				current = current.getSuperClass() ) {
			if ( current.getName().equals( baseType.getName() ) ) {
				return true;
			}
		}
		return false;
	}

	private static ValueNature valueNature(AttributeNature attributeNature) {
		return switch ( attributeNature ) {
			case BASIC -> ValueNature.BASIC;
			case EMBEDDED -> ValueNature.EMBEDDED;
			case TO_ONE -> ValueNature.TO_ONE;
			case ANY -> ValueNature.ANY;
			case ELEMENT_COLLECTION, MANY_TO_ANY, MANY_TO_MANY, ONE_TO_MANY ->
					throw new IllegalArgumentException(
							"Plural attribute nature cannot describe a value: " + attributeNature
					);
		};
	}

	private static CollectionIdMetadataImpl collectionId(
			CollectionSource source,
			CategorizationContext context) {
		final CollectionId collectionId = source.member().getDirectAnnotationUsage( CollectionId.class );
		if ( collectionId == null ) {
			return null;
		}
		final Class<? extends IdentifierGenerator> implementation = collectionId.generatorImplementation();
		if ( implementation != IdentifierGenerator.class ) {
			@SuppressWarnings("unchecked")
			final Class<? extends Generator> generatorClass = (Class<? extends Generator>) implementation;
			return new CollectionIdMetadataImpl(
					collectionId,
					new IdentifierGeneratorRegistration(
							source.member().getDeclaringType().getName() + "#"
									+ source.member().resolveAttributeName(),
							IdentifierGeneratorRegistration.Kind.CUSTOM,
							generatorClass,
							java.util.Map.of()
					)
			);
		}
		return new CollectionIdMetadataImpl(
				collectionId,
				context.getGlobalRegistrations()
						.getIdentifierGeneratorRegistrations()
						.get( collectionId.generator() )
		);
	}

	private static boolean isPlural(AttributeNature nature) {
		return nature == AttributeNature.ELEMENT_COLLECTION
				|| nature == AttributeNature.MANY_TO_MANY
				|| nature == AttributeNature.ONE_TO_MANY
				|| nature == AttributeNature.MANY_TO_ANY;
	}
}
