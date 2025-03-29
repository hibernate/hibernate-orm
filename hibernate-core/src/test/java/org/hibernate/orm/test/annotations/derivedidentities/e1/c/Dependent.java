/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.c;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dependent implements Serializable {

	@Id
	String name;


	@Id
	// id attribute mapped by join column default
	@ManyToOne
	Employee emp;

}
