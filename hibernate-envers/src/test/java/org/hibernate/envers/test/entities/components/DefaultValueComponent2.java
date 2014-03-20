package org.hibernate.envers.test.entities.components;


import javax.persistence.Embeddable;

/**
 * @author Erik-Berndt Scheper
 */
@Embeddable
public class DefaultValueComponent2 {

	private String str1 = "defaultValue";

	private String str2;

	public static final DefaultValueComponent2 of(String str1, String str2) {
		DefaultValueComponent2 instance = new DefaultValueComponent2();
		instance.setStr1( str1 );
		instance.setStr2( str2 );
		return instance;
	}

	public String getStr2() {
		return str2;
	}

	public void setStr2(String str2) {
		this.str2 = str2;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof DefaultValueComponent2) ) {
			return false;
		}

		DefaultValueComponent2 that = (DefaultValueComponent2) o;

		if ( str1 != null ? !str1.equals( that.str1 ) : that.str1 != null ) {
			return false;
		}
		if ( str2 != null ? !str2.equals( that.str2 ) : that.str2 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (str1 != null ? str1.hashCode() : 0);
		result = 31 * result + (str2 != null ? str2.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "Comp2(str1 = " + str1 + ", str2 = " + str2 + ")";
	}

}
