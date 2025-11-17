/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.version;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "T_ORDER" )
public class Order {
	@Id
	public Long id;

	@ManyToOne( cascade = CascadeType.ALL )
	public Customer customer;

	@Version
	public long version;
}
