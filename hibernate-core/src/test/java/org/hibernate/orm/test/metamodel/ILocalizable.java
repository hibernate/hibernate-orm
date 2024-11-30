/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel;

import java.io.Serializable;

interface ILocalizable extends Serializable {
	String getValue();

	void setValue(String value);
}
