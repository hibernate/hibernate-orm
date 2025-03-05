/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.length;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.hibernate.Length.*;

@Entity
public class WithLongStrings {
	@Id
	@GeneratedValue
	public int id;

	@Column(length = LONG)
	public String longish;

	@Column(length = LONG16)
	public String long16;

	@Column(length = LONG32)
	public String long32;

	@Column(length = LOB_DEFAULT+1)
	public String clob;
}
