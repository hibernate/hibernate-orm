/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.wildcard;

import org.hibernate.StatelessSession;
import org.hibernate.annotations.processing.Find;

import java.util.List;

/**
 * A finder parameter may be an extends-bounded wildcard list
 * {@code List<? extends String>} rather than a plain {@code List<String>}, for
 * example when the element type comes from a covariantly-declared collection.
 * Both forms must be treated as a multivalued {@code in} parameter.
 */
public interface BookRepository {

	StatelessSession session();

	// Baseline: a plain list parameter.
	@Find
	List<Book> findByGenre(List<String> genre);

	// The extends-bounded wildcard form.
	@Find
	List<Book> findByGenreWildcard(List<? extends String> genre);
}
