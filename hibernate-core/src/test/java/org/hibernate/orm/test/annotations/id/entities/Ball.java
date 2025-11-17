/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.entities;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

/**
 * Sample of table generator
 *
 * @author Emmanuel Bernard
 */
@TableGenerator(name = "EMP_GEN", table = "GENERATOR_TABLE", pkColumnName = "pkey",
		valueColumnName = "hi", pkColumnValue = "Ball", allocationSize = 10)
@Entity
@SuppressWarnings("serial")
public class Ball implements Serializable {
	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "EMP_GEN")
	@Column(name = "ball_id")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

}
