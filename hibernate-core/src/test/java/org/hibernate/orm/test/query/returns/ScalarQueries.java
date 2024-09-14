/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.returns;

/**
 * @author Steve Ebersole
 */
public interface ScalarQueries {
	String SINGLE_SELECTION_QUERY = "select e.data from BasicEntity e order by e.data";
	String MULTI_SELECTION_QUERY = "select e.id, e.data from BasicEntity e order by e.id";
	String SINGLE_ALIASED_SELECTION_QUERY = "select e.data as state from BasicEntity e order by e.data";
}
