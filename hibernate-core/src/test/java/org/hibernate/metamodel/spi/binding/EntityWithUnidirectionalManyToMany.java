/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

/**
 * @author Gail Badner
 */
@Entity
public class EntityWithUnidirectionalManyToMany {
	private Long id;
	private String name;
	private Collection<SimpleEntity> theBag = new ArrayList<SimpleEntity>();
	private Set<SimpleEntity> theSet = new HashSet<SimpleEntity>();
	private Collection<SimpleEntity> thePropertyRefBag = new ArrayList<SimpleEntity>();

	public EntityWithUnidirectionalManyToMany() {
	}

	public EntityWithUnidirectionalManyToMany(String name) {
		this.name = name;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToMany
	public Collection<SimpleEntity> getTheBag() {
		return theBag;
	}

	public void setTheBag(Collection<SimpleEntity> theBag) {
		this.theBag = theBag;
	}

	@ManyToMany
	public Set<SimpleEntity> getTheSet() {
		return theSet;
	}

	public void setTheSet(Set<SimpleEntity> theSet) {
		this.theSet = theSet;
	}

	@ManyToMany
	public Collection<SimpleEntity> getThePropertyRefSet() {
		return thePropertyRefBag;
	}

	public void setThePropertyRefSet(Set<SimpleEntity> thePropertyRefSet) {
		this.thePropertyRefBag = thePropertyRefSet;
	}
}
