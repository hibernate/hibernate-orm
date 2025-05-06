/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(OrderLinePk.class)
public class OrderLine {
	@Id
	public Order order;
	@Id
	public Product product;
}
