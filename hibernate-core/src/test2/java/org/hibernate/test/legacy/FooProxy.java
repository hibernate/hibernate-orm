/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;


public interface FooProxy {
	public void setNullComponent(FooComponent arg0) throws Exception;
	public FooComponent getNullComponent();
	public void setComponent(FooComponent arg0);
	public FooComponent getComponent();
	public void setCustom(String[] arg0);
	public String[] getCustom();
	public void setBinary(byte[] arg0);
	public byte[] getBinary();
	public void setNullBlob(java.io.Serializable arg0);
	public java.io.Serializable getNullBlob();
	public void setBlob(java.io.Serializable arg0);
	public java.io.Serializable getBlob();
	public void setYesno(boolean arg0);
	public boolean getYesno();
	public boolean equalsFoo(Foo arg0);
	public void disconnect();
	public String toString();
	public void setByte(Byte arg0);
	public Byte getByte();
	public void setNull(Integer arg0);
	public Integer getNull();
	public void setInt(int arg0);
	public int getInt();
	public void setBool(boolean arg0);
	public boolean getBool();
	public void setZero(float arg0);
	public float getZero();
	public void setBytes(byte[] arg0);
	public byte[] getBytes();
	public void setBoolean(Boolean arg0);
	public Boolean getBoolean();
	public void setDouble(Double arg0);
	public Double getDouble();
	public void setFloat(Float arg0);
	public Float getFloat();
	public void setShort(Short arg0);
	public Short getShort();
	public Character getChar();
	public void setChar(Character _char);
	public Long getLong();
	public void setInteger(Integer arg0);
	public Integer getInteger();
	public void setTimestamp(java.util.Date arg0);
	public java.util.Date getTimestamp();
	public void setDate(java.util.Date arg0);
	public java.util.Date getDate();
	public void setString(String arg0);
	public String getString();
	public void setFoo(FooProxy arg0);
	public FooProxy getFoo();
	public void setKey(String arg0);
	public String getKey();
	public Fee getDependent();
	public float getFormula();
}






