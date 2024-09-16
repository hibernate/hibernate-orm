/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.superdao;

import jakarta.data.repository.Repository;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.Pattern;

import java.util.List;

@Repository
public interface SuperRepo {
	@Find
	List<Book> books1(@Pattern String title);

	@HQL("where title like :title")
	List<Book> books2(String title);
}
