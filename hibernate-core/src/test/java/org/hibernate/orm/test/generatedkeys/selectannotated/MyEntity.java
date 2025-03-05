/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.generatedkeys.selectannotated;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;
import org.hibernate.id.SelectGenerator;

/**
 * @author Steve Ebersole
 */
@Entity @Table(name="my_entity")
@GenericGenerator(name = "triggered", type = SelectGenerator.class)
public class MyEntity {
	@Id @GeneratedValue(generator = "triggered")
	@ColumnDefault("-666") //workaround for h2 'before insert' triggers being crap
	private Long id;

	@NaturalId
	private String name;

	public MyEntity() {
	}

	public MyEntity(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
