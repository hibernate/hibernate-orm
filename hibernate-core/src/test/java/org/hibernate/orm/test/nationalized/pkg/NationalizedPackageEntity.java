/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.nationalized.pkg;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "NationalizedPackageEntity")
public class NationalizedPackageEntity {
	@Id
	private Integer id;

	private String name;

	private Character initial;
}
