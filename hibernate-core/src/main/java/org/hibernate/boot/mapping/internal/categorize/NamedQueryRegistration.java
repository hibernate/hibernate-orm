/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.lang.annotation.Annotation;

/// @see org.hibernate.boot.models.JpaAnnotations#NAMED_QUERY
/// @see org.hibernate.boot.models.JpaAnnotations#NAMED_NATIVE_QUERY
/// @see org.hibernate.boot.models.JpaAnnotations#NAMED_STORED_PROCEDURE_QUERY
/// @see org.hibernate.boot.models.HibernateAnnotations#NAMED_QUERY
/// @see org.hibernate.boot.models.HibernateAnnotations#NAMED_NATIVE_QUERY
///
/// @since 9.0
/// @author Steve Ebersole
public record NamedQueryRegistration(
		String name,
		Kind kind,
		boolean isJpa,
		Annotation configuration) {
	public enum Kind {
		HQL,
		NATIVE,
		CALLABLE
	}

	public String getName() {
		return name;
	}

	public Kind getKind() {
		return kind;
	}

	public boolean isJpa() {
		return isJpa;
	}

	public Annotation getConfiguration() {
		return configuration;
	}
}
