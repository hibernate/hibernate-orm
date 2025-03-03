/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.query;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;

/**
 * Example of scalar result (not working right now)
 *
 * @author Emmanuel Bernard
 */
@Entity
@NamedNativeQuery(name = "average", query = "select avg(m.value) from Mark m", resultSetMapping = "columnmapping")
@SqlResultSetMapping(
		name = "columnmapping",
		columns = @ColumnResult(name = "aver")
)
public class Mark {
	@Id
	@GeneratedValue
	public int id;
	public int value;
}
