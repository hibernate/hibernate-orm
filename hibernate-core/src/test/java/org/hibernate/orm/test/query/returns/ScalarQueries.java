/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
