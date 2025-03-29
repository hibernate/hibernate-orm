/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.abstractembeddedcomponents.cid;
import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public interface MyInterface extends Serializable {
	public String getKey1();
	public void setKey1(String key1);
	public String getKey2();
	public void setKey2(String key2);
	public String getName();
	public void setName(String name);
}
