/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;
import java.sql.Blob;
import java.sql.Clob;

public class Blobber {
	private int id;
	private Blob blob;
	private Clob clob;
	/**
	 * Returns the blob.
	 * @return Blob
	 */
	public Blob getBlob() {
		return blob;
	}

	/**
	 * Returns the clob.
	 * @return Clob
	 */
	public Clob getClob() {
		return clob;
	}

	/**
	 * Returns the id.
	 * @return int
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the blob.
	 * @param blob The blob to set
	 */
	public void setBlob(Blob blob) {
		this.blob = blob;
	}

	/**
	 * Sets the clob.
	 * @param clob The clob to set
	 */
	public void setClob(Clob clob) {
		this.clob = clob;
	}

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

}
