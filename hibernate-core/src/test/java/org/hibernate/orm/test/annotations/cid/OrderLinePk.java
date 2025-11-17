/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;
import java.io.Serializable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
public class OrderLinePk implements Serializable {
	@ManyToOne
	@JoinColumn(name = "foo", nullable = false)
	public Order order;
	@ManyToOne
	@JoinColumn(name = "bar", nullable = false)
	public Product product;
}
