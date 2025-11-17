/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Formula;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Address implements Serializable {
	String address1;
	@Column(name = "fld_city")
	String city;
	Country country;
	@ManyToOne
	AddressType type;

	@Formula("1")
	Integer formula;
}
