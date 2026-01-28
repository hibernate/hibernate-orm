/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.BackrefTest;

import java.io.ObjectStreamClass;
import java.io.Serial;

/**
 * @author Paco Hern�ndez
 */
public abstract class CarPart implements java.io.Serializable {

	@Serial
	private static final long serialVersionUID =
			ObjectStreamClass.lookup(CarPart.class).getSerialVersionUID();

	private long id;
	private String partName;

	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return Returns the typeName.
	 */
	public String getPartName() {
		return partName;
	}
	/**
	 * @param typeName The typeName to set.
	 */
	public void setPartName(String typeName) {
		this.partName = typeName;
	}
}
