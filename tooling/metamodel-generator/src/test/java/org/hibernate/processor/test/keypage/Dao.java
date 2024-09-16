/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.keypage;

import jakarta.persistence.EntityManager;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;

public interface Dao {

	EntityManager em();

	@Find
	KeyedResultList<Book> books1(String title, KeyedPage<Book> page);

	@HQL("where title like :title")
	KeyedResultList<Book> books2(String title, KeyedPage<Book> page);
}
