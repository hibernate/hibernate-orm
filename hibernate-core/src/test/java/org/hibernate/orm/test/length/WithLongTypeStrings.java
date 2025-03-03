/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.length;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class WithLongTypeStrings {
	@Id
	@GeneratedValue
	public int id;

	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	public String longish;

	@JdbcTypeCode(SqlTypes.LONG32VARCHAR)
	public String long32;
}
