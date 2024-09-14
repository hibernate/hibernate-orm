/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.namedquery;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.Set;

@Repository(dataStore = "myds")
public interface BookAuthorRepository$ extends BookAuthorRepository {
	@Override
	@Query("from Book where title like :title")
	List<Book> findByTitleLike(String title);
	@Override
	@Query("from Book where type in :types")
	List<Book> findByTypeIn(Set<Book.Type> types);
}
