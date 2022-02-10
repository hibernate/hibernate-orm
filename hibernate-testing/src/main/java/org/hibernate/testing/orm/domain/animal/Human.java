/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.domain.animal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.SortNatural;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;

@Entity
@PrimaryKeyJoinColumn( name = "human_id_fk" )
public class Human extends Mammal {
	private Name name;
	private String nickName;
	private double heightInches;
	
	private BigInteger bigIntegerValue;
	private BigDecimal bigDecimalValue;
	private int intValue;
	private float floatValue;

	private Collection<Human> friends;
	private Collection<DomesticAnimal> pets;
	private Map <String,Human> family;
	private Set<String> nickNames;
	private Map<String,Address> addresses;

	@Embedded
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

	@Column( name = "height_centimeters", nullable = false )
	@ColumnTransformer( read = "height_centimeters / 2.54E0", write = "? * 2.54E0" )
	public double getHeightInches() {
		return heightInches;
	}

	public void setHeightInches(double height) {
		this.heightInches = height;
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

	@ElementCollection
	@CollectionTable( name = "human_nick_names", joinColumns = @JoinColumn( name = "human_fk" ) )
	@Column( name = "nick_name" )
	@SortNatural
	public Set<String> getNickNames() {
		return nickNames;
	}

	public void setNickNames(Set<String> nickNames) {
		this.nickNames = nickNames;
	}

	@ManyToMany
	@JoinTable(
			name = "friends",
			joinColumns = @JoinColumn( name = "friend_fk1" ),
			inverseJoinColumns = @JoinColumn( name = "friend_fk2" )
	)
	public Collection<Human> getFriends() {
		return friends;
	}

	public void setFriends(Collection<Human> friends) {
		this.friends = friends;
	}

	@OneToMany( mappedBy = "owner" )
	public Collection<DomesticAnimal> getPets() {
		return pets;
	}

	public void setPets(Collection<DomesticAnimal> pets) {
		this.pets = pets;
	}

	@ManyToMany
	@JoinTable(
			name = "family",
			joinColumns = @JoinColumn( name = "family_fk1" ),
			inverseJoinColumns = @JoinColumn( name = "family_fk2" )
	)
	@MapKeyColumn( name = "relationship" )
	public Map<String,Human> getFamily() {
		return family;
	}
	

	public void setFamily(Map family) {
		this.family = family;
	}

	@ElementCollection
	@CollectionTable( name = "human_addresses", joinColumns = @JoinColumn( name = "human_fk" ) )
	@MapKeyColumn( name = "`type`" )
	public Map<String,Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(Map<String,Address> addresses) {
		this.addresses = addresses;
	}
	
	
}
