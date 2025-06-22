/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.ordered.joinedInheritence;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity
public class Tiger extends Animal {
	private int numberOfStripes;

	private Details details;

	public int getNumberOfStripes() {
		return numberOfStripes;
	}

	public void setNumberOfStripes(int numberOfStripes) {
		this.numberOfStripes = numberOfStripes;
	}

	public Details getDetails() {
		return details;
	}

	public void setDetails(Details details) {
		this.details = details;
	}

	@Embeddable
	public static class Details {
		private String name;
		private String sex;

		public String getSex() {
			return sex;
		}

		public void setSex(String sex) {
			this.sex = sex;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
