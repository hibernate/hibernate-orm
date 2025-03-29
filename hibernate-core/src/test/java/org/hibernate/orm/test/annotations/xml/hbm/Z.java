/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.hbm;


/**
 * @author Emmanuel Bernard
 */
public interface Z extends java.io.Serializable {
public Integer getZId();

public void setZId(Integer zId);

public B getB();

public void setB(B b);
}
