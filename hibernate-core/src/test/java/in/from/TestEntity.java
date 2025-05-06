/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package in.from;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class TestEntity {
	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@ManyToOne
	private Any any;

	public TestEntity() {
	}

	public TestEntity( String name, Any any) {
		this.name = name;
		this.any = any;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Any getAny() {
		return any;
	}
}
