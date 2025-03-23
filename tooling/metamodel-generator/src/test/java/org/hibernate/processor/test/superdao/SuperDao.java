/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.superdao;

import jakarta.persistence.EntityManager;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.Pattern;

import java.util.List;

public interface SuperDao {

	EntityManager em();


	@Find
	List<Book> books1(@Pattern String title);

	@HQL("where title like :title")
	List<Book> books2(String title);
}
