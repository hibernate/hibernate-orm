/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadataImplementor;
import org.hibernate.boot.mapping.internal.categorize.EmbeddableUsageMetadataImpl;
import org.hibernate.boot.mapping.internal.categorize.EmbeddedValueMetadataImpl;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeVariableScope;

import jakarta.annotation.Nullable;
import jakarta.persistence.AccessType;
import jakarta.persistence.Access;
import org.hibernate.boot.model.source.spi.AttributePath;

/// Binding-model contribution for one embeddable/component usage path.
///
/// The contribution captures the path-sensitive source facts for an embeddable
/// usage: component role, declaring member, component type, type-variable scope,
/// access fallback, and the ordered source members selected for this usage.
///
/// @since 9.0
/// @author Steve Ebersole
public class EmbeddableContribution {
	private final ComponentSource.Kind kind;
	private final @Nullable MemberDetails sourceMember;
	private final ClassDetails componentType;
	private final TypeVariableScope typeVariableScope;
	private final AccessType defaultAccessType;
	private final String pathPrefix;
	private final String namingPathPrefix;
	private final List<ComponentMemberBinding> members;
	private final EmbeddableDiscriminatorSource discriminator;
	private final org.hibernate.boot.mapping.spi.EmbeddableUsageMetadata usage;

	public EmbeddableContribution(
			ComponentSource.Kind kind,
			@Nullable MemberDetails sourceMember,
			ClassDetails componentType,
			TypeVariableScope typeVariableScope,
			AccessType defaultAccessType,
			String pathPrefix,
			String namingPathPrefix,
			List<ComponentMemberBinding> members,
			EmbeddableDiscriminatorSource discriminator,
			org.hibernate.boot.mapping.spi.EmbeddableUsageMetadata usage) {
		this.kind = kind;
		this.sourceMember = sourceMember;
		this.componentType = componentType;
		this.typeVariableScope = typeVariableScope;
		this.defaultAccessType = defaultAccessType;
		this.pathPrefix = pathPrefix;
		this.namingPathPrefix = namingPathPrefix;
		this.members = List.copyOf( members );
		this.discriminator = discriminator;
		this.usage = usage;
	}

	/// Creates a contribution for a synthetic component structure not represented
	/// by categorized embeddable metadata, principally `CompositeUserType`
	/// representations.
	public static EmbeddableContribution from(
			ComponentSource source,
			BindingState bindingState,
			BindingContext bindingContext) {
		final List<ComponentMemberBinding> members = new ArrayList<>();
		source.members()
				.stream()
				.map( member -> ComponentMemberBinding.from( source, member, bindingState, bindingContext ) )
				.forEach( members::add );
		if ( source.kind() != ComponentSource.Kind.EMBEDDED_IDENTIFIER ) {
			source.subclassMembers( bindingContext )
					.stream()
					.map( member -> ComponentMemberBinding.from( source, member, bindingState, bindingContext ) )
					.forEach( members::add );
		}
		return new EmbeddableContribution(
				source.kind(),
				source.sourceMember(),
				source.componentType(),
				source.typeVariableScope(),
				source.defaultAccessType(),
				source.pathPrefix(),
				source.namingPathPrefix(),
				members,
				EmbeddableDiscriminatorSource.from( source, bindingContext ),
				new SyntheticEmbeddableUsage( source )
		);
	}

	public static EmbeddableContribution from(
			ComponentSource source,
			EmbeddedValueMetadataImpl valueMetadata,
			BindingState bindingState,
			BindingContext bindingContext) {
		final EmbeddableUsageMetadataImpl usage = valueMetadata.getEmbeddableUsage();
		final List<ComponentMemberBinding> members = categorizedMembers(
				source,
				usage,
				bindingState,
				bindingContext
		);
		if ( source.kind() != ComponentSource.Kind.EMBEDDED_IDENTIFIER ) {
			for ( EmbeddableUsageMetadataImpl subtypeUsage : valueMetadata.getSubtypeUsages() ) {
				final String subtypeName = subtypeUsage.type().getClassDetails().getName();
				for ( AttributeMetadataImplementor attribute : subtypeUsage.attributes() ) {
					if ( attribute.getMember().getDeclaringType().getName().equals( subtypeName ) ) {
						members.add(
								ComponentMemberBinding.from(
										source,
										componentMember( source, subtypeUsage, attribute ),
										attribute,
										bindingState,
										bindingContext
								)
						);
					}
				}
			}
		}
		return contribution( source, usage, members, bindingContext );
	}

	private static List<ComponentMemberBinding> categorizedMembers(
			ComponentSource source,
			EmbeddableUsageMetadataImpl usage,
			BindingState bindingState,
			BindingContext bindingContext) {
		final List<ComponentMemberBinding> members = new ArrayList<>( usage.attributes().size() );
		for ( AttributeMetadataImplementor attribute : usage.attributes() ) {
			final ComponentSource.ComponentMember member = componentMember( source, usage, attribute );
			members.add(
					ComponentMemberBinding.from(
							source,
							member,
							attribute,
							bindingState,
							bindingContext
					)
			);
		}
		return members;
	}

	private static EmbeddableContribution contribution(
			ComponentSource source,
			EmbeddableUsageMetadataImpl usage,
			List<ComponentMemberBinding> members,
			BindingContext bindingContext) {
		return new EmbeddableContribution(
				source.kind(),
				source.sourceMember(),
				source.componentType(),
				usage.typeVariableScope(),
				usage.accessType(),
				source.pathPrefix(),
				source.namingPathPrefix(),
				members,
				EmbeddableDiscriminatorSource.from( source, bindingContext ),
				usage
		);
	}

	public org.hibernate.boot.mapping.spi.EmbeddableUsageMetadata usage() {
		return usage;
	}

	private record SyntheticEmbeddableUsage(
			org.hibernate.boot.mapping.spi.EmbeddableTypeMetadata type,
			MemberDetails sourceMember,
			TypeVariableScope typeVariableScope,
			AccessType accessType,
			List<? extends org.hibernate.boot.mapping.spi.AttributeMetadata> attributes)
			implements org.hibernate.boot.mapping.spi.EmbeddableUsageMetadata {
		private SyntheticEmbeddableUsage(ComponentSource source) {
			this(
					new SyntheticEmbeddableType( source.componentType(), source.defaultAccessType() ),
					source.sourceMember(),
					source.typeVariableScope(),
					source.defaultAccessType(),
					List.of()
			);
		}
	}

	private record SyntheticEmbeddableType(ClassDetails getClassDetails, AccessType getExplicitAccessType)
			implements org.hibernate.boot.mapping.spi.EmbeddableTypeMetadata {
	}

	private static ComponentSource.ComponentMember componentMember(
			ComponentSource source,
			EmbeddableUsageMetadataImpl usage,
			AttributeMetadataImplementor attribute) {
		final MemberDetails member = attribute.getMember();
		final String attributeName = attribute.getName();
		final String path = source.pathPrefix() + attributeName;
		final String fullPath = source.namingPathPrefix() + attributeName;
		final Access access = member.getDeclaringType().getDirectAnnotationUsage( Access.class );
		return new ComponentSource.ComponentMember(
				member,
				attribute.resolveAttributeType( source.typeVariableScope() ),
				AttributePath.parse( path ),
				path,
				AttributePath.parse( fullPath ),
				fullPath,
				member.getDeclaringType(),
				access == null ? usage.accessType() : access.value()
		);
	}

	public ComponentSource.Kind kind() {
		return kind;
	}

	public @Nullable MemberDetails sourceMember() {
		return sourceMember;
	}

	public ClassDetails componentType() {
		return componentType;
	}

	public TypeVariableScope typeVariableScope() {
		return typeVariableScope;
	}

	public AccessType defaultAccessType() {
		return defaultAccessType;
	}

	public String pathPrefix() {
		return pathPrefix;
	}

	public String namingPathPrefix() {
		return namingPathPrefix;
	}

	public List<ComponentMemberBinding> members() {
		return members;
	}

	public EmbeddableDiscriminatorSource discriminator() {
		return discriminator;
	}
}
