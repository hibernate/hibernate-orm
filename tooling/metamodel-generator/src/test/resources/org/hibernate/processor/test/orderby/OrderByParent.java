/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.orderby;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
public class OrderByParent {

	@Id
	long id;

	@OneToMany
	@OrderBy(OrderByChild_.SEQ_NO)
	List<OrderByChild> children;
}
