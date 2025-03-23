/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel;

import java.io.Serializable;

interface ILocalizable extends Serializable {
	String getValue();

	void setValue(String value);
}
