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
package org.hibernate.test.dirtiness;

import java.util.*;
import javax.persistence.*;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
public class Thing implements CustomDirtyCheckable {
	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( strategy = "increment", name = "increment" )
	private Long id;

	private String name;
	private Date mutableProperty;

	@OneToMany( cascade = CascadeType.ALL, mappedBy = "parent", orphanRemoval = true )
	private Set<ChildThing> children;

	public Thing() {
	}

	public Thing(String name) {
		this.name = name;
	}

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
		// intentionally simple dirty tracking (i.e. no checking against previous state)
		changedValues.put( "name", this.name );
		this.name = name;
	}

	public Date getMutableProperty() {
		return mutableProperty;
	}

	public void setMutableProperty(Date mutableProperty) {
		// intentionally simple dirty tracking (i.e. no checking against previous state)
		changedValues.put( "mutableProperty", this.mutableProperty );
		this.mutableProperty = mutableProperty;
	}
	
	public Set<ChildThing> getChildren() {
		return children;
	}

	public void setChildren(Set<ChildThing> children) {
		this.children = children;
	}

	@Override
	public Map<String, Object> getChangedValues() {
		return changedValues;
	}

	@Transient
	private Map<String,Object> changedValues = new HashMap<String, Object>();
}
