/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone.alphabetical;
import java.math.BigInteger;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Droitacces {
	@Id
	private BigInteger idpk;

	@ManyToOne
	private Benefserv idpkbenef;
}
