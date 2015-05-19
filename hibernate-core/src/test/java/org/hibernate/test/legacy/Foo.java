/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Foo.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.classic.Lifecycle;

public class Foo implements Lifecycle, FooProxy, Serializable {

	private static int count=0;

	public static class Struct implements java.io.Serializable {
		String name;
		int count;
		public boolean equals(Object other) {
			Struct s = (Struct) other;
			return ( s.name==name || s.name.equals(name) ) && s.count==count;
		}
		public int hashCode() {
			return count;
		}
	}

	/*public boolean equals(Object other) {
		FooProxy otherFoo = (FooProxy) other;
		return this.key.equals( otherFoo.getKey() ) && this._string.equals( otherFoo.getString() );
	}

	public int hashCode() {
		return key.hashCode() - _string.hashCode();
	}*/

	String key;
	FooProxy _foo;
	String _string;
	Date _date;
	Date _timestamp;
	Integer _integer;
	Long _long;
	Short _short;
	Float _float;
	Double _double;
	Boolean _boolean;
	Byte _byte;
	Integer _null;
	int _int;
	boolean _bool;
	float _zero;
	byte[] _bytes;
	boolean yesno;
	java.io.Serializable blob;
	java.io.Serializable nullBlob;
	byte[] binary;
	String[] custom;
	FooComponent component;
	char _char;
	Fee dependent;
	Locale theLocale;
	private int version;
	private Timestamp versionTimestamp;
	private Calendar versionCalendar;
	private float formula;
	private String joinedProp;

	private int x;

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}

	public Foo() {
	}

	public Foo(String key) {
		this.key = key;
	}

	public Foo(int x) {
		this.x=x;
	}

	public boolean onSave(Session db) throws CallbackException {
		_string = "a string";
		_date = new Date(123);
		_timestamp = new Date( System.currentTimeMillis() );
		_integer = new Integer( -666 );
		_long = new Long( 696969696969696969l - count++ );
		_short = new Short("42");
		_float = new Float( 6666.66f );
		//_double = new Double( 1.33e-69 );  // this double is too big for the sap db jdbc driver
		_double = new Double( 1.12e-36 );
		_boolean = new Boolean(true);
		_byte = new Byte( (byte) 127 );
		_int = 2;
		_char = '@';
		_bytes = _string.getBytes();
		Struct s = new Struct();
		s.name="name";
		s.count = 69;
		blob = s;
		binary = ( _string + "yada yada yada" ).getBytes();
		custom = new String[] { "foo", "bar" };
		component = new FooComponent("foo", 12, new Date[] { _date, _timestamp, null, new Date() }, new FooComponent("bar", 666, new Date[] { new Date(123456l), null }, null ) );
		component.setGlarch( new Glarch() );
		dependent = new Fee();
		dependent.setFi( "belongs to foo # " + getKey() );
		theLocale = Locale.getDefault();
		return NO_VETO;
	}

	public boolean onDelete(Session db) throws CallbackException {
		return NO_VETO;
	}
	public boolean onUpdate(Session db) throws CallbackException {
		return NO_VETO;
	}

	public void onLoad(Session db, Serializable id) {
	}

	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}

	public FooProxy getFoo() {
		return _foo;
	}
	public void setFoo(FooProxy foo) {
		_foo = foo;
	}

	public String getString() {
		return _string;
	}
	public void setString(String string) {
		_string = string;
		//if (_foo!=null) _foo.setString(string);
	}

	public java.util.Date getDate() {
		return _date;
	}
	public void setDate(java.util.Date date) {
		_date = date;
	}

	public java.util.Date getTimestamp() {
		return _timestamp;
	}
	public void setTimestamp(java.util.Date timestamp) {
		_timestamp = timestamp;
	}

	public Integer getInteger() {
		return _integer;
	}
	public void setInteger(Integer iinteger) {
		_integer = iinteger;
	}
	public Long getLong() {
		return _long;
	}
	public void setLong(Long llong) {
		_long = llong;
	}


	public Short getShort() {
		return _short;
	}
	public void setShort(Short sshort) {
		_short = sshort;
	}
	public Float getFloat() {
		return _float;
	}
	public void setFloat(Float ffloat) {
		_float = ffloat;
	}
	public Double getDouble() {
		return _double;
	}
	public void setDouble(Double ddouble) {
		_double = ddouble;
	}
	public Boolean getBoolean() {
		return _boolean;
	}
	public void setBoolean(Boolean bboolean) {
		_boolean = bboolean;
	}
	public byte[] getBytes() {
		return _bytes;
	}
	public void setBytes(byte[] bytes) {
		_bytes = bytes;
	}
	public float getZero() {
		return _zero;
	}
	public void setZero(float zero) {
		_zero = zero;
	}
	public boolean getBool() {
		return _bool;
	}
	public void setBool(boolean bool) {
		_bool = bool;
	}

	public int getInt() {
		return _int;
	}
	public void setInt(int iint) {
		_int = iint;
	}

	public Integer getNull() {
		return _null;
	}
	public void setNull(Integer nnull) {
		_null = nnull;
	}

	public Byte getByte() {
		return _byte;
	}

	public void setByte(Byte bbyte) {
		_byte = bbyte;
	}

	public String toString() {
		return this.getClass().getName() + ": " + key;
	}

	public void disconnect() {
		if ( _foo!=null) _foo.disconnect();
		_foo=null;
	}

	public boolean equalsFoo(Foo other) {
		if ( _bytes!=other._bytes ) {
			if ( _bytes==null || other._bytes==null ) return false;
			if ( _bytes.length!=other._bytes.length ) return false;
			for ( int i=0; i< _bytes.length; i++) {
				if ( _bytes[i] != other._bytes[i] ) return false;
			}
		}

		return ( this._bool == other._bool )
		&& ( ( this._boolean == other._boolean ) || ( this._boolean.equals(other._boolean) ) )
		&& ( ( this._byte == other._byte ) || ( this._byte.equals(other._byte) ) )
		//&& ( ( this._date == other._date ) || ( this._date.getDate() == other._date.getDate() && this._date.getMonth() == other._date.getMonth() && this._date.getYear() == other._date.getYear() ) )
		&& ( ( this._double == other._double ) || ( this._double.equals(other._double) ) )
		&& ( ( this._float == other._float ) || ( this._float.equals(other._float) ) )
		&& ( this._int == other._int )
		&& ( ( this._integer == other._integer ) || ( this._integer.equals(other._integer) ) )
		&& ( ( this._long == other._long ) || ( this._long.equals(other._long) ) )
		&& ( this._null == other._null )
		&& ( ( this._short == other._short ) || ( this._short.equals(other._short) ) )
		&& ( ( this._string == other._string) || ( this._string.equals(other._string) ) )
		//&& ( ( this._timestamp==other._timestamp) || ( this._timestamp.getDate() == other._timestamp.getDate() && this._timestamp.getYear() == other._timestamp.getYear() && this._timestamp.getMonth() == other._timestamp.getMonth() ) )
		&& ( this._zero == other._zero )
		&& ( ( this._foo == other._foo ) || ( this._foo.getKey().equals( other._foo.getKey() ) ) )
		&& ( ( this.blob == other.blob ) || ( this.blob.equals(other.blob) ) )
		&& ( this.yesno == other.yesno )
		&& ( ( this.binary == other.binary ) || java.util.Arrays.equals(this.binary, other.binary) )
		&& ( this.key.equals(other.key) )
		&& ( this.theLocale.equals(other.theLocale) )
		&& ( ( this.custom == other.custom ) || ( this.custom[0].equals(other.custom[0]) && this.custom[1].equals(other.custom[1]) ) );

	}

	public boolean getYesno() {
		return yesno;
	}

	public void setYesno(boolean yesno) {
		this.yesno = yesno;
	}

	public java.io.Serializable getBlob() {
		return blob;
	}

	public void setBlob(java.io.Serializable blob) {
		this.blob = blob;
	}

	public java.io.Serializable getNullBlob() {
		return nullBlob;
	}

	public void setNullBlob(java.io.Serializable nullBlob) {
		this.nullBlob = nullBlob;
	}

	public byte[] getBinary() {
		return binary;
	}
	public void setBinary(byte[] binary) {
		this.binary = binary;
	}

	public String[] getCustom() {
		return custom;
	}

	public void setCustom(String[] custom) {
		this.custom = custom;
	}

	public FooComponent getComponent() {
		return component;
	}
	public void setComponent(FooComponent component) {
		this.component = component;
	}

	public FooComponent getNullComponent() {
		return null;
	}
	public void setNullComponent(FooComponent fc) throws Exception {
		if (fc!=null) throw new Exception("Null component");
	}

	public Character getChar() {
		return new Character(_char);
	}

	public void setChar(Character _char) {
		this._char = _char.charValue();
	}

	public Fee getDependent() {
		return dependent;
	}

	public void setDependent(Fee dependent) {
		this.dependent = dependent;
	}

	/**
	 * Returns the locale.
	 * @return Locale
	 */
	public Locale getLocale() {
		return theLocale;
	}

	/**
	 * Sets the locale.
	 * @param locale The locale to set
	 */
	public void setLocale(Locale locale) {
		this.theLocale = locale;
	}

	/**
	 * Returns the version.
	 * @return int
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * Sets the version.
	 * @param version The version to set
	 */
	public void setVersion(int version) {
		this.version = version;
	}

	/**
	 * Returns the versionTimestamp.
	 * @return Timestamp
	 */
	public Timestamp getVersionTimestamp() {
		return versionTimestamp;
	}

	/**
	 * Sets the versionTimestamp.
	 * @param versionTimestamp The versionTimestamp to set
	 */
	public void setVersionTimestamp(Timestamp versionTimestamp) {
		this.versionTimestamp = versionTimestamp;
	}

	public void finalize() { }

	public Calendar getVersionCalendar() {
		return versionCalendar;
	}

	public void setVersionCalendar(Calendar calendar) {
		versionCalendar = calendar;
	}

	public float getFormula() {
		return formula;
	}

	public void setFormula(float f) {
		formula = f;
	}

	/**
	 * @return Returns the joinedProp.
	 */
	public String getJoinedProp() {
		return joinedProp;
	}

	/**
	 * @param joinedProp The joinedProp to set.
	 */
	public void setJoinedProp(String joinedProp) {
		this.joinedProp = joinedProp;
	}

}





