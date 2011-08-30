/*
  * Hibernate, Relational Persistence for Idiomatic Java
  *
  * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-
  * party contributors as indicated by the @author tags or express 
  * copyright attribution statements applied by the authors.  
  * All third-party contributions are distributed under license by 
  * Red Hat, Inc.
  *
  * This copyrighted material is made available to anyone wishing to 
  * use, modify, copy, or redistribute it subject to the terms and 
  * conditions of the GNU Lesser General Public License, as published 
  * by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of 
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public 
  * License along with this distribution; if not, write to:
  * 
  * Free Software Foundation, Inc.
  * 51 Franklin Street, Fifth Floor
  * Boston, MA  02110-1301  USA
  */

package org.hibernate.test.annotations.manytoonewithformula;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="MODEL")
public class Model {
	
	private ModelId id;
	
	private String name;
	
	public Model(ModelId id, String name) {
		this.id = id;
		this.name = name;
	}
		
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Model other = (Model) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public Model() {
	}
	
	@Id
	public ModelId getId() {
		return id;
	}
	
	public void setId(ModelId id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}

