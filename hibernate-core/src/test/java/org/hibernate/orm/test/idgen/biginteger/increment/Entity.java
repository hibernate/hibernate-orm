/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.biginteger.increment;
import java.math.BigInteger;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class Entity {
	private BigInteger id;
	private String name;

	public Entity() {
	}

	public Entity(String name) {
		this.name = name;
	}

	public BigInteger getId() {
		return id;
	}

	public void setId(BigInteger id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
