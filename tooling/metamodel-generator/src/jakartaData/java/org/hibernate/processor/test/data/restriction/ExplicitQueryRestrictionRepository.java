/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.restriction;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import org.hibernate.query.restriction.Restriction;

import java.util.List;

@Repository
public interface ExplicitQueryRestrictionRepository {
	record Summary(String isbn, String title) {
	}

	@Query("from DataRestrictionBook")
	List<Summary> books(Restriction<DataRestrictionBook> restriction);
}
