/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.Table;

/**
 * @author Brett Meyer
 */
@Entity
@Table(name="TableA")
@SecondaryTables({@SecondaryTable(name = "TableB")})
public class EntityWithNestedEmbeddables {
	@Id
	@GeneratedValue
	private Integer id;

	@Embedded
	private EmbeddableA embedA;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public EmbeddableA getEmbedA() {
		return embedA;
	}

	public void setEmbedA(EmbeddableA embedA) {
		this.embedA = embedA;
	}
}
