/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.version;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity
public class Customer {
	@Id
	public Long id;

	@OneToMany( fetch = FetchType.EAGER, mappedBy = "customer", cascade = CascadeType.ALL )
	public List<Order> orders = new ArrayList<Order>();

	@Version
	public long version;
}
