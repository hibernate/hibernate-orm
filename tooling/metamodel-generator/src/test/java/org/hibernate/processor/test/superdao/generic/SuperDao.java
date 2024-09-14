/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.superdao.generic;

import jakarta.persistence.EntityManager;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.Pattern;

import java.util.List;

public interface SuperDao<T,K> {

    EntityManager em();

    @Find
    T get(K isbn);

    @Find
    List<T> books1(@Pattern String title);

    @HQL("where title like :title")
    List<T> books2(String title);

}
