/*
 * SPDX-License-Identifier: Apache-2.0
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
