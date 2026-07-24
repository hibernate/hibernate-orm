/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeVariableScope;

import jakarta.annotation.Nullable;
import jakarta.persistence.AccessType;

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

	public EmbeddableContribution(
			ComponentSource.Kind kind,
			@Nullable MemberDetails sourceMember,
			ClassDetails componentType,
			TypeVariableScope typeVariableScope,
			AccessType defaultAccessType,
			String pathPrefix,
			String namingPathPrefix,
			List<ComponentMemberBinding> members,
			EmbeddableDiscriminatorSource discriminator) {
		this.kind = kind;
		this.sourceMember = sourceMember;
		this.componentType = componentType;
		this.typeVariableScope = typeVariableScope;
		this.defaultAccessType = defaultAccessType;
		this.pathPrefix = pathPrefix;
		this.namingPathPrefix = namingPathPrefix;
		this.members = List.copyOf( members );
		this.discriminator = discriminator;
	}

	public static EmbeddableContribution from(
			ComponentSource source,
			BindingState bindingState,
			BindingContext bindingContext) {
		final List<ComponentMemberBinding> members = new ArrayList<>();
		source.members()
				.stream()
				.map( member -> ComponentMemberBinding.from( source, member, bindingState, bindingContext ) )
				.forEach( members::add );
		source.subclassMembers( bindingContext )
				.stream()
				.map( member -> ComponentMemberBinding.from( source, member, bindingState, bindingContext ) )
				.forEach( members::add );
		return new EmbeddableContribution(
				source.kind(),
				source.sourceMember(),
				source.componentType(),
				source.typeVariableScope(),
				source.defaultAccessType(),
				source.pathPrefix(),
				source.namingPathPrefix(),
				members,
				EmbeddableDiscriminatorSource.from( source, bindingContext )
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
