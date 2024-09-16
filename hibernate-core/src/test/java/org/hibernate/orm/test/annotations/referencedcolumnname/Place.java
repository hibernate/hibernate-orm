/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
