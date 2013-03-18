/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.manytomany.compositepk;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.hibernate.annotations.ForeignKey;

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
					}
	)
	@ForeignKey(name = "WM_W_FK", inverseName = "WM_M_FK")
	public Set<Man> getMens() {
		return mens;
	}

	public void setMens(Set<Man> mens) {
		this.mens = mens;
	}

	@EmbeddedId
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
