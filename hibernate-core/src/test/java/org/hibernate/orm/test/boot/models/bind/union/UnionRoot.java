/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.union;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * @author Steve Ebersole
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class UnionRoot {
	@Id
	private Integer id;

	private String name;
}
