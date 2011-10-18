package org.hibernate.test.hql;

import java.io.Serializable;

public class CompositeIdEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long key1;
	private String key2;
	private String someProperty;

	public Long getKey1() {
		return key1;
	}

	public void setKey1( Long key1 ) {
		this.key1 = key1;
	}

	public String getKey2() {
		return key2;
	}

	public void setKey2( String key2 ) {
		this.key2 = key2;
	}

	public String getSomeProperty() {
		return someProperty;
	}

	public void setSomeProperty( String someProperty ) {
		this.someProperty = someProperty;
	}

	@Override
	public int hashCode() {
		// not really needed, thus the dumb implementation.
		return 42;
	}

	@Override
	public boolean equals( Object obj ) {
		if (this == obj) {
			return true;
		}
		if ( !( obj instanceof CompositeIdEntity ) ) {
			return false; 
		}
		CompositeIdEntity other = ( CompositeIdEntity ) obj;
		if ( key1 == null ? other.key1 != null : !key1.equals( other.key1 ) ) {
			return false;
		}
		if ( key2 == null ? other.key2 != null : !key2.equals( other.key2 ) ) {
			return false;
		}
		return true;
	}
}
