/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: SecureDocument.java 8670 2005-11-25 17:36:29Z epbernard $

package org.hibernate.test.mixed;



/**
 * @author Gavin King
 */

public class SecureDocument extends Document {

	private byte permissionBits;

	private String owner;

	/**
	 * @return Returns the owner.
	 */

	public String getOwner() {

		return owner;

	}

	/**
	 * @param owner The owner to set.
	 */

	public void setOwner(String owner) {

		this.owner = owner;

	}

	/**
	 * @return Returns the permissionBits.
	 */

	public byte getPermissionBits() {

		return permissionBits;

	}

	/**
	 * @param permissionBits The permissionBits to set.
	 */

	public void setPermissionBits(byte permissionBits) {

		this.permissionBits = permissionBits;

	}

}

