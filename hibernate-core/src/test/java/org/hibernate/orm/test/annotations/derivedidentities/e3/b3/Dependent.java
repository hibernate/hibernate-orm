/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.b3;

import jakarta.persistence.*;

@Entity
@Table(name="`Dependent`")
public class Dependent {

	@EmbeddedId
	DependentId id;

	@JoinColumn(name = "FIRSTNAME", referencedColumnName = "FIRSTNAME")
	@JoinColumn(name = "LASTNAME", referencedColumnName = "lastName")
	@MapsId("empPK")
	@ManyToOne
	Employee emp;
}
