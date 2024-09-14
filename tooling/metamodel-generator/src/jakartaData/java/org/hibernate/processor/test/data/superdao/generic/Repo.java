/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.superdao.generic;

import jakarta.data.repository.Repository;
import org.hibernate.annotations.processing.Find;

@Repository
public interface Repo extends SuperRepo<Book,String> {
    @Find
	Book get(String isbn);
}
