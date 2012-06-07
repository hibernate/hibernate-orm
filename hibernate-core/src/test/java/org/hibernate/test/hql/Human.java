//$Id: Human.java 9873 2006-05-04 13:42:48Z max.andersen@jboss.com $
package org.hibernate.test.hql;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Human extends Mammal {
	private Name name;
	private String nickName;
	private Collection friends;
	private Collection pets;
	private Map family;
	private double heightInches;
	
	private BigInteger bigIntegerValue;
	private BigDecimal bigDecimalValue;
	private int intValue;
	private float floatValue;
	
	private Set nickNames;
	private Map addresses;

	public Collection getFriends() {
		return friends;
	}

	public void setFriends(Collection friends) {
		this.friends = friends;
	}

	public Collection getPets() {
		return pets;
	}

	public void setPets(Collection pets) {
		this.pets = pets;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	
	public double getHeightInches() {
		return heightInches;
	}
	
	public void setHeightInches(double height) {
		this.heightInches = height;
	}

	public Map getFamily() {
		return family;
	}
	

	public void setFamily(Map family) {
		this.family = family;
	}

	public Set getNickNames() {
		return nickNames;
	}

	public void setNickNames(Set nickNames) {
		this.nickNames = nickNames;
	}

	public Map getAddresses() {
		return addresses;
	}

	public void setAddresses(Map addresses) {
		this.addresses = addresses;
	}

	public BigDecimal getBigDecimalValue() {
		return bigDecimalValue;
	}

	public void setBigDecimalValue(BigDecimal bigDecimalValue) {
		this.bigDecimalValue = bigDecimalValue;
	}

	public BigInteger getBigIntegerValue() {
		return bigIntegerValue;
	}

	public void setBigIntegerValue(BigInteger bigIntegerValue) {
		this.bigIntegerValue = bigIntegerValue;
	}

	public float getFloatValue() {
		return floatValue;
	}

	public void setFloatValue(float floatValue) {
		this.floatValue = floatValue;
	}

	public int getIntValue() {
		return intValue;
	}

	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}
	
	
}
