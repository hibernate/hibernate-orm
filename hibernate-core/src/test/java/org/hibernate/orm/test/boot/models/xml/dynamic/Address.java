/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.dynamic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class Address {
	@Id
	private Integer id;
	private String street;
	private String city;
	private String state;
	private String zip;
}
