/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

import java.io.Serializable;
import java.util.List;

public class ComponentCollection implements Serializable {
	private String str;
	private List foos;
	private List floats;
	public List getFoos() {
		return foos;
	}

	public String getStr() {
		return str;
	}

	public void setFoos(List list) {
		foos = list;
	}

	public void setStr(String string) {
		str = string;
	}

	public List getFloats() {
		return floats;
	}

	public void setFloats(List list) {
		floats = list;
	}

}
