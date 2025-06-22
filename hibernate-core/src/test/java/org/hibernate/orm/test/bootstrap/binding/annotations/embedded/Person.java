/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import java.io.Serializable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "PersonEmbed")
public class Person implements Serializable {
	@Id
	@GeneratedValue
	Integer id;

	String name;

	@Embedded
	Address address;

	@Embedded
	AddressBis addressBis;

	@Embedded
	@AttributeOverrides( {
			@AttributeOverride(name = "iso2", column = @Column(name = "bornIso2")),
			@AttributeOverride(name = "name", column = @Column(name = "bornCountryName"))
	})
	Country bornIn;
}
