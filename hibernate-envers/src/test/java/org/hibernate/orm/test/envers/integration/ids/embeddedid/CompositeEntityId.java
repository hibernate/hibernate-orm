/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

@Embeddable
public class CompositeEntityId implements Serializable{

	private String firstCode;
	private String secondCode;

	public String getFirstCode() {
		return firstCode;
	}

	public void setFirstCode(String firstCode) {
		this.firstCode = firstCode;
	}

	public String getSecondCode() {
		return secondCode;
	}

	public void setSecondCode(String secondCode) {
		this.secondCode = secondCode;
	}

	@Override
	public String toString() {
		return "CompositeEntityId{" +
				"firstCode='" + firstCode + '\'' +
				", secondCode='" + secondCode + '\'' +
				'}';
	}
}
