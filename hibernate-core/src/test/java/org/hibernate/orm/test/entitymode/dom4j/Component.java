/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitymode.dom4j;

/**
 * @author Steve Ebersole
 */
public class Component {
	private String something;
	private String somethingElse;

	public String getSomething() {
		return something;
	}

	public void setSomething(String something) {
		this.something = something;
	}

	public String getSomethingElse() {
		return somethingElse;
	}

	public void setSomethingElse(String somethingElse) {
		this.somethingElse = somethingElse;
	}
}
