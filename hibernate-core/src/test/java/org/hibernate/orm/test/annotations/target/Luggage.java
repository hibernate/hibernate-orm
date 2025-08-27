/*
 * SPDX-License-Identifier: Apache-2.0
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
