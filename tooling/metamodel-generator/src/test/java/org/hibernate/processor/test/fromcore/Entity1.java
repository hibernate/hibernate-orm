/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.fromcore;

import jakarta.persistence.*;

@Entity
@Table(name = "entity1")
public class Entity1 {
	@Id
	private long id;

	@ManyToOne
	@JoinColumn(name="entity2_id", nullable = false)
	private Entity2 entity2;

	@Column(name = "val")
	private String value;
}
