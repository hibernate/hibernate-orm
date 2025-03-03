/*
 * SPDX-License-Identifier: Apache-2.0
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
