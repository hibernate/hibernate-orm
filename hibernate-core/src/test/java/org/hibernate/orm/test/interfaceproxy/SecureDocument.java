/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interfaceproxy;


/**
 * @author Gavin King
 */
public interface SecureDocument extends Document {
	/**
	 * @return Returns the owner.
	 */
	public String getOwner();

	/**
	 * @param owner The owner to set.
	 */
	public void setOwner(String owner);

	/**
	 * @return Returns the permissionBits.
	 */
	public byte getPermissionBits();

	/**
	 * @param permissionBits The permissionBits to set.
	 */
	public void setPermissionBits(byte permissionBits);
}
