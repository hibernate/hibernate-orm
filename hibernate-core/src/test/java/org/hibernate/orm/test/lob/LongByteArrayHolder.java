/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;


/**
 * An entity containing data that is materialized into a byte array immediately.
 * The hibernate type mapped for {@link #longByteArray} determines the SQL type
 * actually used.
 *
 * @author Gail Badner
 */
public class LongByteArrayHolder {
	private Long id;
	private byte[] longByteArray;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public byte[] getLongByteArray() {
		return longByteArray;
	}

	public void setLongByteArray(byte[] longByteArray) {
		this.longByteArray = longByteArray;
	}
}
