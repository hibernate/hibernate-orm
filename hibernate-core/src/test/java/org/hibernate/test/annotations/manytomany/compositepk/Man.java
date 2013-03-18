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
import javax.persistence.ManyToMany;

/**
 * Man knowing sevezral womens
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Man implements Serializable {
	private ManPk id;
	private String carName;
	private Set<Woman> womens;

	@ManyToMany(cascade = {CascadeType.ALL}, mappedBy = "mens")
	public Set<Woman> getWomens() {
		return womens;
	}

	public void setWomens(Set<Woman> womens) {
		this.womens = womens;
	}

	@EmbeddedId
	public ManPk getId() {
		return id;
	}

	public void setId(ManPk id) {
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
		if ( obj != null && obj instanceof Man ) {
			return getId().equals( ( (Man) obj ).getId() );
		}
		else {
			return false;
		}
	}

}
