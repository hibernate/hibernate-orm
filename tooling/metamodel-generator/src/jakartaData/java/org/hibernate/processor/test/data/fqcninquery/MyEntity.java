/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.fqcninquery;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQuery(name = "#getName",
		query = "select name from org.hibernate.processor.test.data.fqcninquery.MyEntity where id=:id")
@NamedQuery(name = "#getUniqueId",
		query = "select uniqueId from MyEntity where name=:name")
public class MyEntity {

	@Id
	Integer id;

	String name;

	String uniqueId;
}
