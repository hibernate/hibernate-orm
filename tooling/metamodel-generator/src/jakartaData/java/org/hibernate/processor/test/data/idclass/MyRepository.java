/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.idclass;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import org.hibernate.processor.test.data.idclass.MyEntity.MyEntityId;

@Repository
public interface MyRepository {

	@Query("from MyEntity where id=:id")
	MyEntity findById(MyEntityId id);
}
