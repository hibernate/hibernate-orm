/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "PHONE")
public class Phone {
	@Id
	private Long id;

	private String number;
}
