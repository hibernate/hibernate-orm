/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.hbm;

/**
 * @author Emmanuel Bernard
 */
public interface A extends java.io.Serializable {
	public Integer getAId();

	public void setAId(Integer aId);

	String getDescription();

	void setDescription(String description);
}
