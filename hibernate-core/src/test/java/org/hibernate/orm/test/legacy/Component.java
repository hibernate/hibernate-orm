/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


/**
 * Component
 *
 * @author Emmanuel Bernard
 */
public class Component {
	private String _name;

	private SubComponent _subComponent;

	/**
	 * @return
	 */
	public String getName() {
		return _name;
	}

	/**
	 * @param string
	 */
	public void setName(String string) {
		_name = string;
	}

	/**
	 * @return
	 */
	public SubComponent getSubComponent() {
		return _subComponent;
	}

	/**
	 * @param component
	 */
	public void setSubComponent(SubComponent component) {
		_subComponent = component;
	}

}
