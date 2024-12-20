/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
