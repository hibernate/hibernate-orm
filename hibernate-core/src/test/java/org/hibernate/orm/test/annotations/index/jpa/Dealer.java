/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.index.jpa;

import java.io.Serializable;
import jakarta.persistence.Embeddable;

/**
 * @author Strong Liu
 */
@Embeddable
public class Dealer implements Serializable {
	private String name;
	private long rate;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getRate() {
		return rate;
	}

	public void setRate(long rate) {
		this.rate = rate;
	}
}
