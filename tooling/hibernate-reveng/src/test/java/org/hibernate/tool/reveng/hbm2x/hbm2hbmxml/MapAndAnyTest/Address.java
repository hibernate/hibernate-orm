/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.MapAndAnyTest;

import java.util.HashSet;
import java.util.Set;

public class Address {
	private Long id;
	private Set<String> lines = new HashSet<String>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<String> getLines() {
		return lines;
	}

	public void setLines(Set<String> lines) {
		this.lines = lines;
	}
}
