/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.query;
import jakarta.persistence.MappedSuperclass;

@org.hibernate.annotations.NamedQuery(
		name = "night.olderThan",
		query = "select n from Night n where n.date <= :date"
)

@MappedSuperclass
public class Darkness {

}
