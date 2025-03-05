/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.referencedcolumnname;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Janario Oliveira
 */
@Entity
public class Place {

	@Id
	@GeneratedValue
	int id;
	@Column(name = "NAME")
	String name;
	@Column(name = "OWNER")
	String owner;
}
