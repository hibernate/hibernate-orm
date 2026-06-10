/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "HELLO_WORLD")
public class HelloWorld {

	@Id
	@Column(length = 10)
	private String id;

	@Column(length = 5)
	private String hello;

	private long world;
}
