/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.dao;

import java.util.List;

import org.hibernate.annotations.processing.HQL;

import jakarta.persistence.EntityManager;

public interface Dao2 {

	EntityManager getEntityManager();

	// Simple name
	@HQL("from Book b where b.type = Magazine")
	List<Book> findMagazines1();

	// Simple qualified name
	@HQL("from Book b where b.type = Type.Magazine")
	List<Book> findMagazines2();

	// Canonical FQN
	@HQL("from Book b where b.type = org.hibernate.processor.test.dao.Book.Type.Magazine")
	List<Book> findMagazines3();

	// Binary FQN
	@HQL("from Book b where b.type = org.hibernate.processor.test.dao.Book$Type.Magazine")
	List<Book> findMagazines4();
}
