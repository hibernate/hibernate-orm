/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone.alphabetical;
import java.math.BigInteger;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;


@Entity
public class Service {
	@Id
	private BigInteger idpk;
}
