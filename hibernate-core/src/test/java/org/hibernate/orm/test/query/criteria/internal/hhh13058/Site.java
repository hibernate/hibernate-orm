/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh13058;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@Entity(name = "Site")
@Table(name = "Site")
public class Site {

	@Id
	@GeneratedValue
	Long id;

}
