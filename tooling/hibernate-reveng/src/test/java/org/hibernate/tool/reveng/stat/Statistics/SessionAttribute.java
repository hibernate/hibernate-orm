/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.stat.Statistics;


/**
 * @author Gavin King
 */
public class SessionAttribute {
	private Long id;
	private String name;
	private String stringData;

	SessionAttribute() {}
	public SessionAttribute(String name) {
		this.name = name;
	}
	public SessionAttribute(String name, String str) {
		this.name = name;
		this.stringData = str;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getStringData() {
		return stringData;
	}
	public void setStringData(String stringData) {
		this.stringData = stringData;
	}

	public Long getId() {
		return id;
	}
}
