/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.target;


/**
 * @author Emmanuel Bernard
 */
public interface Luggage {
	double getHeight();
	double getWidth();

	void setHeight(double height);
	void setWidth(double width);

	Owner getOwner();

	void setOwner(Owner owner);
}
