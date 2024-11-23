/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
