/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.hbm;


/**
 * @author Emmanuel Bernard
 */
public interface B extends A {
	public Integer getBId();

	public void setBId(Integer bId);
}
