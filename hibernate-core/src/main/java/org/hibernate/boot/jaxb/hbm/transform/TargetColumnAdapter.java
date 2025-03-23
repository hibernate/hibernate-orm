/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

/**
 * @author Steve Ebersole
 */
interface TargetColumnAdapter {
	void setName(String value);

	void setTable(String value);

	void setNullable(Boolean value);

	void setUnique(Boolean value);

	void setColumnDefinition(String value);

	void setLength(Integer value);

	void setPrecision(Integer value);

	void setScale(Integer value);

	void setDefault(String value);

	void setCheck(String value);

	void setComment(String value);

	void setRead(String value);

	void setWrite(String value);

	void setInsertable(Boolean value);

	void setUpdatable(Boolean value);
}
