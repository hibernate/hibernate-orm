/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package ee.jakarta.tck.data.framework.read.only;

import jakarta.annotation.Generated;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository(dataStore = "")
@Generated("ee.jakarta.tck.data.tools.annp.RepositoryProcessor")
interface CustomRepository$ extends CustomRepository {
	@Override
	@Query("select count(this) as Integer where id in ?1")
	public long countByIdIn(java.util.Set<java.lang.Long> ids);

	@Override
	@Query("select count(this)>0 where id in ?1")
	public boolean existsByIdIn(java.util.Set<java.lang.Long> ids);


	// TODO; Implement TCK overrides
}
