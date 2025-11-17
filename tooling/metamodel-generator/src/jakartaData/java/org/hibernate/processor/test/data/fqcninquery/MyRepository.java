/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.fqcninquery;


import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface MyRepository {

	@Query("select name from org.hibernate.processor.test.data.fqcninquery.MyEntity where id=:id")
	String getName(Integer id);


	@Query("select uniqueId from MyEntity where name=:name")
	String getUniqueId(String name);
}
