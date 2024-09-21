/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.components;


/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface IComponent {
	String getData();

	void setData(String data);
}
