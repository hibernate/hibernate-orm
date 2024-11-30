/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.bidirectional;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Hardy Ferentschik
 */
@Entity
//@IdClass(DependentId.class)
public class Dependent implements Serializable {
	@Id
	@ManyToOne
	Employee emp;

	String name;
}
