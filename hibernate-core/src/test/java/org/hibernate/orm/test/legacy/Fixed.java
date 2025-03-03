/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Fixed extends Broken {
	private Set set;
	private List list = new ArrayList();

	public Set getSet() {
		return set;
	}

	public void setSet(Set set) {
		this.set = set;
	}

	public List getList() {
		return list;
	}

	public void setList(List list) {
		this.list = list;
	}

}
