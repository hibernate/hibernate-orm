/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.view;

import java.util.List;

import org.hibernate.boot.models.bind.internal.model.EmbeddableContribution;
import org.hibernate.boot.models.bind.internal.sources.ComponentSource;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeVariableScope;

import jakarta.annotation.Nullable;
import jakarta.persistence.AccessType;

/// Stable read view over one applied embeddable contribution.
///
/// @since 9.0
/// @author Steve Ebersole
public record EmbeddableContributionView(EmbeddableContribution contribution) {
	public ComponentSource.Kind kind() {
		return contribution.kind();
	}

	public @Nullable MemberDetails sourceMember() {
		return contribution.sourceMember();
	}

	public ClassDetails componentType() {
		return contribution.componentType();
	}

	public TypeVariableScope typeVariableScope() {
		return contribution.typeVariableScope();
	}

	public AccessType defaultAccessType() {
		return contribution.defaultAccessType();
	}

	public String pathPrefix() {
		return contribution.pathPrefix();
	}

	public String namingPathPrefix() {
		return contribution.namingPathPrefix();
	}

	public List<ComponentSource.ComponentMember> members() {
		return contribution.members();
	}
}
