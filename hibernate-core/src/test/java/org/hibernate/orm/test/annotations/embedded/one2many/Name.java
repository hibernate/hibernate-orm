/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embedded.one2many;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;

import jakarta.persistence.Access;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Embeddable
@Access(AccessType.PROPERTY)
public class Name {
	private String first;
	private String last;

	public Name() {
	}

	public Name(String first, String last) {
		this.first = first;
		this.last = last;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}
}
