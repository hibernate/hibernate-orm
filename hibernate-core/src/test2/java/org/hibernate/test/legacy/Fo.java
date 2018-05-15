/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Fo.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
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







