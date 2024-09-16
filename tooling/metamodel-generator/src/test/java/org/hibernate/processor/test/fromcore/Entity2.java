/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.fromcore;

import jakarta.persistence.*;

@Entity
@Table(name = "entity2")
public class Entity2 {
	@Id
	private long id;

	@ManyToOne
	@JoinColumn(name="entity3_id")
	private Entity3 entity3;

	@Column(name = "val")
	private String value;
}
