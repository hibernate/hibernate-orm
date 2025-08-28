/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.io.Serializable;

public final class Fo {

	public static Fo newFo(FumCompositeID id) {
		Fo fo = newFo();
		fo.id = id;
		return fo;
	}

	public static Fo newFo() {
		return new Fo();
	}

	private Fo() {}

	private FumCompositeID id;
	private byte[] buf;
	private Serializable serial;
	private long version;
	private int x;

	public FumCompositeID getId() {
		return id;
	}

	public void setId(FumCompositeID id) {
		this.id = id;
	}

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}

	public byte[] getBuf() {
		return buf;
	}


	public Serializable getSerial() {
		return serial;
	}


	public void setBuf(byte[] buf) {
		this.buf = buf;
	}


	public void setSerial(Serializable serial) {
		this.serial = serial;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

}
