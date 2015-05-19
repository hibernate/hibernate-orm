/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dirtiness;

import java.util.*;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
public class Thing {
	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( strategy = "increment", name = "increment" )
	private Long id;

	private String name;
	private Date mutableProperty;

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
		this.mutableProperty = mutableProperty;
	}

	@Transient
	Map<String,Object> changedValues = new HashMap<String, Object>();
}
