/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

/**
 * Sub component
 *
 * @author emmanuel
 */
public class SubComponent {
	private String _subName;

	private String _subName1;

	/**
	 * @return
	 */
	public String getSubName() {
		return _subName;
	}

	/**
	 * @param string
	 */
	public void setSubName(String string) {
		_subName = string;
	}

	/**
	 * @return
	 */
	public String getSubName1() {
		return _subName1;
	}

	/**
	 * @param string
	 */
	public void setSubName1(String string) {
		_subName1 = string;
	}

}
