/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import org.hibernate.StatelessSession;
import org.hibernate.processor.test.integ.model.Author;

import java.util.List;

@Repository
public interface AuthorRepository {

	StatelessSession session();

	@Insert
	void insert(Author author);

	@Save
	void save(Author author);

	@Save
	void saveAll(List<Author> authors);

	@Find
	Author byId(Long id);
}
