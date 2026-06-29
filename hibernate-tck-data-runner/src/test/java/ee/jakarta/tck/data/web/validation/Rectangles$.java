/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package ee.jakarta.tck.data.web.validation;

import jakarta.annotation.Generated;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository(dataStore = "")
@Generated("ee.jakarta.tck.data.tools.annp.RepositoryProcessor")
interface Rectangles$ extends Rectangles {
	@Override
	@Query("select count(this)")
	public @jakarta.validation.constraints.PositiveOrZero long countAll();


	// TODO; Implement TCK overrides
}
