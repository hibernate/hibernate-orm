/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.util.Set;

/**
 * @author Gavin King
 */
public class K {
	private Long id;
	private Set is;
	void setIs(Set is) {
		this.is = is;
	}
	Set getIs() {
		return is;
	}
}
