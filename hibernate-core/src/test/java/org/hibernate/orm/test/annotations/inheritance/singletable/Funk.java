/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.singletable;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorValue("1")
public class Funk extends Music {
	private int starred;

	public int getStarred() {
		return starred;
	}

	public void setStarred(int starred) {
		this.starred = starred;
	}
}
