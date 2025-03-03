/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * TODO: change this sample with an Address -> Country relation. This is more accurate
 *
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "tbl_child")
public class Child implements Serializable {
	@Id
	@GeneratedValue
	public Integer id;

	@ManyToOne
	@JoinColumn(name = "parentCivility", referencedColumnName = "isMale")
	@JoinColumn(name = "parentLastName", referencedColumnName = "lastName")
	@JoinColumn(name = "parentFirstName", referencedColumnName = "firstName")
	public Parent parent;
}
