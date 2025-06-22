/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.type.dynamicparameterized;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Daniel Gredler
 */
@MappedSuperclass
public abstract class AbstractEntity {

	@Id
	@Temporal(TemporalType.DATE)
	@Column(name = "ID")
	Date id;

}
