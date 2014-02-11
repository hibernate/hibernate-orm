package org.hibernate.test.collection.set;

import java.io.Serializable;

public class PersonInfo implements Serializable {

	private static final long serialVersionUID = -1180462304958757873L;

	private int height;
	private String hairColor;

	public PersonInfo() {

	}

	public PersonInfo(final int height, final String hairColor) {
		if ( height <= 0 ) {
			throw new IllegalArgumentException( "Wrong height" );
		}
		this.height = height;
		this.hairColor = hairColor;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		if ( height <= 0 ) {
			throw new IllegalArgumentException( "Wrong height" );
		}
		this.height = height;
	}

	public String getHairColor() {
		return hairColor;
	}

	public void setHairColor(String hairColor) {
		this.hairColor = hairColor;
	}

	@Override
	public boolean equals(Object obj) {
		final boolean result;
		if ( obj == this ) {
			result = true;
		}
		else if ( obj instanceof PersonInfo ) {
			final PersonInfo other = (PersonInfo) obj;
			result = getHeight() == other.getHeight() && getHairColor() == null ? other.getHairColor() == null : getHairColor().equals( other.getHairColor() );
		}
		else {
			result = false;
		}
		return result;
	}

	@Override
	public int hashCode() {
		return 31 * ( 31 * getHeight() + ( getHairColor() == null ? 0 : getHairColor().hashCode() ) );
	}

	@Override
	public String toString() {
		return "PersonInfo{height=" + getHeight() + ",hairColor=" + getHairColor() + "}";
	}

}
