/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;
import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 * Woman knowing several mens
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Woman implements Serializable {
	private WomanPk id;
	private String carName;
	private Set<Man> mens;
	private Set<Cat> cats;

	@ManyToMany(mappedBy = "humanContacts")
	public Set<Cat> getCats() {
		return cats;
	}

	public void setCats(Set<Cat> cats) {
		this.cats = cats;
	}

	@ManyToMany(cascade = {CascadeType.ALL})
	@JoinTable(
			name = "Man_Woman",
			joinColumns = {
			@JoinColumn(name = "womanLastName", referencedColumnName = "lastName"),
			@JoinColumn(name = "womanFirstName", referencedColumnName = "firstName")
					},
			inverseJoinColumns = {
			@JoinColumn(name = "manIsElder", referencedColumnName = "elder"),
			@JoinColumn(name = "manLastName", referencedColumnName = "lastName"),
			@JoinColumn(name = "manFirstName", referencedColumnName = "firstName")
					},
			foreignKey = @ForeignKey(name = "WM_W_FK")
)
	public Set<Man> getMens() {
		return mens;
	}

	public void setMens(Set<Man> mens) {
		this.mens = mens;
	}

	@Id
	public WomanPk getId() {
		return id;
	}

	public void setId(WomanPk id) {
		this.id = id;
	}

	public String getCarName() {
		return carName;
	}

	public void setCarName(String carName) {
		this.carName = carName;
	}


	public int hashCode() {
		//a NPE can occurs, but I don't expect hashcode to be used before pk is set
		return getId().hashCode();
	}

	public boolean equals(Object obj) {
		//a NPE can occurs, but I don't expect equals to be used before pk is set
		if ( obj != null && obj instanceof Woman ) {
			return getId().equals( ( (Woman) obj ).getId() );
		}
		else {
			return false;
		}
	}

}
