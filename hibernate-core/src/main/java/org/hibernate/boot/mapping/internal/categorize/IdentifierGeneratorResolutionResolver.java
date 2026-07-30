/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.UUID;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.model.IdentifierGeneratorRegistration;
import org.hibernate.boot.model.internal.GeneratorAnnotationHelper;
import org.hibernate.boot.model.internal.GeneratorParameters;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.generator.Generator;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.MemberDetails;

import static org.hibernate.boot.models.HibernateAnnotations.UUID_GENERATOR;
import static org.hibernate.boot.models.JpaAnnotations.SEQUENCE_GENERATOR;
import static org.hibernate.boot.models.JpaAnnotations.TABLE_GENERATOR;
import static org.hibernate.boot.model.internal.GeneratorStrategies.resolveLegacyGeneratorClass;

/// Resolves identifier-generator declarations into the categorized hierarchy
/// model for later identifier materialization.
final class IdentifierGeneratorResolutionResolver {
	private IdentifierGeneratorResolutionResolver() {
	}

	static IdentifierGeneratorResolution resolve(
			EntityHierarchyImpl hierarchy,
			KeyMapping idMapping,
			CategorizationContext context) {
		final ArrayList<IdentifierGeneratorResolution.Part> parts = new ArrayList<>();
		idMapping.forEachAttribute( (index, attribute) -> {
			resolveAttribute( hierarchy, attribute, context, parts );
		} );
		return new IdentifierGeneratorResolution( parts );
	}

	private static void resolveAttribute(
			EntityHierarchyImpl hierarchy,
			AttributeMetadataImplementor attribute,
			CategorizationContext context,
			ArrayList<IdentifierGeneratorResolution.Part> parts) {
		final IdentifierGeneratorResolution.Part part = resolvePart( hierarchy, attribute, context );
		if ( part != null ) {
			parts.add( part );
		}
		if ( attribute instanceof EmbeddedAttributeMetadataImpl embeddedAttribute ) {
			for ( AttributeMetadataImplementor nestedAttribute :
					embeddedAttribute.getValue().getEmbeddableUsage().attributes() ) {
				resolveAttribute( hierarchy, nestedAttribute, context, parts );
			}
		}
	}

	private static IdentifierGeneratorResolution.Part resolvePart(
			EntityHierarchyImpl hierarchy,
			AttributeMetadataImplementor attribute,
			CategorizationContext context) {
		final MemberDetails member = attribute.getMember();
		final Annotation generatorAnnotation = findGeneratorAnnotation( hierarchy, member, context );
		if ( generatorAnnotation != null ) {
			final IdGeneratorType generatorType = generatorAnnotation.annotationType()
					.getAnnotation( IdGeneratorType.class );
			final IdentifierGeneratorRegistration.Kind kind =
					generatorAnnotation.annotationType() == UuidGenerator.class
							? IdentifierGeneratorRegistration.Kind.UUID
							: IdentifierGeneratorRegistration.Kind.CUSTOM;
			final Class<? extends Generator> generatorClass =
					generatorAnnotation instanceof GenericGenerator genericGenerator
							? genericGenerator.type()
							: generatorType.value();
			return IdentifierGeneratorResolution.Part.generator(
					attribute,
					new IdentifierGeneratorRegistration(
							hierarchy.getRoot().getJpaEntityName(),
							kind,
							generatorClass,
							java.util.Map.of()
					),
					generatorAnnotation
			);
		}

		final GeneratedValue generatedValue = member.getDirectAnnotationUsage( GeneratedValue.class );
		if ( generatedValue == null ) {
			return null;
		}
		if ( "assigned".equals( generatedValue.generator() ) ) {
			return registered(
					attribute,
					new IdentifierGeneratorRegistration(
							generatedValue.generator(),
							IdentifierGeneratorRegistration.Kind.CUSTOM,
							org.hibernate.id.Assigned.class,
							java.util.Map.of()
					),
					null
			);
		}

		return switch ( generatedValue.strategy() ) {
			case IDENTITY -> IdentifierGeneratorResolution.Part.identity( attribute );
			case UUID -> uuid( hierarchy, attribute, member, context );
			case SEQUENCE -> resolveSequence(
					hierarchy,
					attribute,
					member,
					generatedValue,
					context
			);
			case TABLE -> resolveTable(
					hierarchy,
					attribute,
					member,
					generatedValue,
					context
			);
			case AUTO -> resolveAuto( hierarchy, attribute, member, generatedValue, context );
		};
	}

	private static IdentifierGeneratorResolution.Part resolveAuto(
			EntityHierarchyImpl hierarchy,
			AttributeMetadataImplementor attribute,
			MemberDetails member,
			GeneratedValue generatedValue,
			CategorizationContext context) {
		final String registrationName = registrationName( hierarchy, generatedValue );
		final SequenceGenerator localizedSequence = GeneratorAnnotationHelper.findLocalizedMatch(
				SEQUENCE_GENERATOR,
				member,
				hierarchy.getRoot().getClassDetails(),
				SequenceGenerator::name,
				registrationName,
				context.getModelsContext()
		);
		if ( localizedSequence != null ) {
			return registered(
					attribute,
					sequenceRegistration( registrationName, localizedSequence ),
					localizedSequence
			);
		}

		final TableGenerator localizedTable = GeneratorAnnotationHelper.findLocalizedMatch(
				TABLE_GENERATOR,
				member,
				hierarchy.getRoot().getClassDetails(),
				TableGenerator::name,
				registrationName,
				context.getModelsContext()
		);
		if ( localizedTable != null ) {
			return registered(
					attribute,
					tableRegistration( registrationName, localizedTable ),
					localizedTable
			);
		}

		final IdentifierGeneratorRegistration globalRegistration =
				context.getGlobalRegistrations().getIdentifierGeneratorRegistrations().get( registrationName );
		if ( globalRegistration != null ) {
			return registered( attribute, globalRegistration, null );
		}

		if ( !generatedValue.generator().isBlank() ) {
			final Class<? extends Generator> legacyGeneratorClass = resolveLegacyGeneratorClass(
					generatedValue.generator(),
					context.getDatabase().getDialect()
			);
			if ( legacyGeneratorClass != null ) {
				return registered(
						attribute,
						new IdentifierGeneratorRegistration(
								registrationName,
								IdentifierGeneratorRegistration.Kind.CUSTOM,
								legacyGeneratorClass,
								java.util.Map.of()
						),
						null
				);
			}
		}

		if ( member.getType().isImplementor( UUID.class )
				|| member.getType().isImplementor( String.class ) ) {
			return uuid( hierarchy, attribute, member, context );
		}

		final SequenceGenerator implicit =
				new SequenceGeneratorJpaAnnotation( generatedValue.generator(), context.getModelsContext() );
		return registered( attribute, sequenceRegistration( registrationName, implicit ), implicit );
	}

	private static IdentifierGeneratorResolution.Part resolveSequence(
			EntityHierarchyImpl hierarchy,
			AttributeMetadataImplementor attribute,
			MemberDetails member,
			GeneratedValue generatedValue,
			CategorizationContext context) {
		final String registrationName = registrationName( hierarchy, generatedValue );
		final SequenceGenerator localized = GeneratorAnnotationHelper.findLocalizedMatch(
				SEQUENCE_GENERATOR,
				member,
				hierarchy.getRoot().getClassDetails(),
				generatedValue.generator().isBlank() ? null : SequenceGenerator::name,
				generatedValue.generator().isBlank() ? null : generatedValue.generator(),
				context.getModelsContext()
		);
		if ( localized != null ) {
			return registered( attribute, sequenceRegistration( registrationName, localized ), localized );
		}

		final IdentifierGeneratorRegistration globalRegistration =
				context.getGlobalRegistrations().getIdentifierGeneratorRegistrations().get( registrationName );
		if ( globalRegistration != null
				&& globalRegistration.getKind() == IdentifierGeneratorRegistration.Kind.SEQUENCE
		) {
			return registered( attribute, globalRegistration, null );
		}

		final SequenceGenerator implicit =
				new SequenceGeneratorJpaAnnotation( generatedValue.generator(), context.getModelsContext() );
		return registered( attribute, sequenceRegistration( registrationName, implicit ), implicit );
	}

	private static IdentifierGeneratorResolution.Part resolveTable(
			EntityHierarchyImpl hierarchy,
			AttributeMetadataImplementor attribute,
			MemberDetails member,
			GeneratedValue generatedValue,
			CategorizationContext context) {
		final String registrationName = registrationName( hierarchy, generatedValue );
		final TableGenerator localized = GeneratorAnnotationHelper.findLocalizedMatch(
				TABLE_GENERATOR,
				member,
				hierarchy.getRoot().getClassDetails(),
				generatedValue.generator().isBlank() ? null : TableGenerator::name,
				generatedValue.generator().isBlank() ? null : generatedValue.generator(),
				context.getModelsContext()
		);
		if ( localized != null ) {
			return registered( attribute, tableRegistration( registrationName, localized ), localized );
		}

		final IdentifierGeneratorRegistration globalRegistration =
				context.getGlobalRegistrations().getIdentifierGeneratorRegistrations().get( registrationName );
		if ( globalRegistration != null
				&& globalRegistration.getKind() == IdentifierGeneratorRegistration.Kind.TABLE
		) {
			return registered( attribute, globalRegistration, null );
		}

		final TableGenerator implicit =
				new TableGeneratorJpaAnnotation( generatedValue.generator(), context.getModelsContext() );
		return registered( attribute, tableRegistration( registrationName, implicit ), implicit );
	}

	private static IdentifierGeneratorRegistration sequenceRegistration(
			String registrationName,
			SequenceGenerator configuration) {
		final IdentifierGeneratorRegistration.Builder builder = new IdentifierGeneratorRegistration.Builder();
		builder.setKind( IdentifierGeneratorRegistration.Kind.SEQUENCE );
		GeneratorParameters.interpretSequenceGenerator( configuration, builder );
		builder.setName( registrationName );
		return builder.build();
	}

	private static IdentifierGeneratorRegistration tableRegistration(
			String registrationName,
			TableGenerator configuration) {
		final IdentifierGeneratorRegistration.Builder builder = new IdentifierGeneratorRegistration.Builder();
		builder.setKind( IdentifierGeneratorRegistration.Kind.TABLE );
		GeneratorParameters.interpretTableGenerator( configuration, builder );
		builder.setName( registrationName );
		return builder.build();
	}

	private static IdentifierGeneratorResolution.Part uuid(
			EntityHierarchyImpl hierarchy,
			AttributeMetadataImplementor attribute,
			MemberDetails member,
			CategorizationContext context) {
		final UuidGenerator configuration = GeneratorAnnotationHelper.findLocalizedMatch(
				UUID_GENERATOR,
				member,
				hierarchy.getRoot().getClassDetails(),
				null,
				null,
				context.getModelsContext()
		);
		return registered(
				attribute,
				new IdentifierGeneratorRegistration(
						hierarchy.getRoot().getJpaEntityName(),
						IdentifierGeneratorRegistration.Kind.UUID,
						org.hibernate.id.uuid.UuidGenerator.class,
						java.util.Map.of()
				),
				configuration
		);
	}

	private static IdentifierGeneratorResolution.Part registered(
			AttributeMetadataImplementor attribute,
			IdentifierGeneratorRegistration registration,
			Annotation configuration) {
		return IdentifierGeneratorResolution.Part.generator( attribute, registration, configuration );
	}

	private static String registrationName(EntityHierarchyImpl hierarchy, GeneratedValue generatedValue) {
		return generatedValue.generator().isBlank()
				? hierarchy.getRoot().getJpaEntityName()
				: generatedValue.generator();
	}

	private static Annotation findGeneratorAnnotation(
			EntityHierarchyImpl hierarchy,
			MemberDetails member,
			CategorizationContext context) {
		Annotation match = findGeneratorAnnotation( member, context );
		if ( match != null ) {
			return match;
		}

		final AnnotationTarget entityType = hierarchy.getRoot().getClassDetails();
		if ( !entityType.getName().equals( member.getDeclaringType().getName() ) ) {
			match = findGeneratorAnnotation( entityType, context );
			if ( match != null ) {
				return match;
			}
		}

		match = findGeneratorAnnotation( member.getDeclaringType(), context );
		if ( match != null ) {
			return match;
		}

		final var packageInfo = GeneratorAnnotationHelper.locatePackageInfoDetails(
				member.getDeclaringType(),
				context.getModelsContext()
		);
		return packageInfo == null ? null : findGeneratorAnnotation( packageInfo, context );
	}

	private static Annotation findGeneratorAnnotation(
			AnnotationTarget target,
			CategorizationContext context) {
		final var annotations = target.getMetaAnnotated( IdGeneratorType.class, context.getModelsContext() );
		if ( annotations.size() > 1 ) {
			throw new org.hibernate.AnnotationException(
					"Identifier generator target '" + target.getName()
							+ "' declares multiple generator annotations " + annotations
			);
		}
		return annotations.isEmpty() ? null : annotations.get( 0 );
	}
}
