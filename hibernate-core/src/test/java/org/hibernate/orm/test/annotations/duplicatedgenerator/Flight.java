/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.duplicatedgenerator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Here to test duplicate import
 *
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "tbl_flight")
public class Flight {
	@Id
	public String id;
}
