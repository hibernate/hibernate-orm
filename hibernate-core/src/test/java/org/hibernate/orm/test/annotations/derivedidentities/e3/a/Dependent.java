/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.a;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "`Dependent`")
@IdClass(DependentId.class)
public class Dependent {
	@Id
	@Column(name = "dep_name")
	String name; // default column name is overridden

	@Id
	@JoinColumn(name = "FK1", referencedColumnName = "firstName")
	@JoinColumn(name = "FK2", referencedColumnName = "lastName")
	@ManyToOne
	Employee emp;
}
