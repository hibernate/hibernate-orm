/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.elementcollection;

import java.util.ArrayList;
import java.util.Collection;

public class Owner {

	private Long id;

	private Collection<Revision> trace = new ArrayList<>();

	public Owner() {
	}

	public Owner(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Collection<Revision> getTrace() {
		return trace;
	}

	public void setTrace(Collection<Revision> trace) {
		this.trace = trace;
	}
}
