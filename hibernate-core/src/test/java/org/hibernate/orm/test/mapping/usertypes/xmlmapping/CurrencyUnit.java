/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.usertypes.xmlmapping;

public interface CurrencyUnit{
	String getCurrencyCode();

	int getNumericCode();
}
