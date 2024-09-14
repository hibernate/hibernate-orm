/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.namedquery;

import jakarta.data.repository.Repository;
import org.hibernate.processor.test.data.namedquery.Book.Type;

import java.util.List;
import java.util.Set;

@Repository(dataStore = "myds")
public interface BookAuthorRepository {
	List<Book> findByTitleLike(String title);
	List<Book> findByTypeIn(Set<Type> types);
}
