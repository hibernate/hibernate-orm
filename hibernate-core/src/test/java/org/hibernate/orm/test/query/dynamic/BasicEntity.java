/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.dynamic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "BasicEntity")
@Table(name = "BasicEntity")
public class BasicEntity {
	@Id
	private Integer id;
	private String name;
	private int position;

	@ManyToOne
	OtherEntity other;
}
