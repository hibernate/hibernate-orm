/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.nullable;

import org.hibernate.StatelessSession;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

/**
 * A finder method may be marked nullable using {@code jakarta.annotation.Nullable},
 * {@code org.jetbrains.annotations.Nullable}, or {@code org.jspecify.annotations.Nullable}.
 * All three forms must produce a generated method that returns {@code null}
 * instead of throwing when no result is found.
 */
public interface BookRepository {

	StatelessSession session();

	@jakarta.annotation.Nullable
	@HQL("from Book where isbn = :isbn")
	Book findByIsbnNullableJakarta(String isbn);

	@org.jetbrains.annotations.Nullable
	@HQL("from Book where isbn = :isbn")
	Book findByIsbnNullableJetbrains(String isbn);

	@HQL("from Book where isbn = :isbn")
	@org.jspecify.annotations.Nullable Book findByIsbnNullableJspecify(String isbn);

	// a type-use annotation on the return type that is not org.jspecify.annotations.Nullable,
	// so hasJspecifyNullableAnnotation() must iterate past a non-matching annotation mirror
	@HQL("from Book where isbn = :isbn")
	@org.jspecify.annotations.NonNull Book findByIsbnNotNullable(String isbn);

	// a @Find finder on a @NaturalId attribute, exercising the NATURAL_ID
	// finder strategy's call to hasNullableAnnotation()
	@Find
	Book findByTitle(String title);
}
