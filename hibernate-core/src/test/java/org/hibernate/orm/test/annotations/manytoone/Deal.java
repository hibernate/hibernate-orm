/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Deal {
	@Id @GeneratedValue public Integer id;
	@ManyToOne @JoinColumn(referencedColumnName = "userId") public Customer from;
	@ManyToOne @JoinColumn(referencedColumnName = "userId") public Customer to;

}
