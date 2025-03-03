/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.usertypes.xmlmapping;

public interface CurrencyUnit{
	String getCurrencyCode();

	int getNumericCode();
}
