/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;

/**
 * @author Andrea Boriero
 */
@Entity
public class EntityWithManyToOneJoinTable {
	private Integer id;

	// alphabetical
	private String name;
	private SimpleEntity other;
	private Integer someInteger;
	private BasicEntity lazyOther;

	public EntityWithManyToOneJoinTable() {
	}

	public EntityWithManyToOneJoinTable(Integer id, String name, Integer someInteger) {
		this.id = id;
		this.name = name;
		this.someInteger = someInteger;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	@JoinTable(name = "ENTITY_OTHER",
			joinColumns =  {
					@JoinColumn( name = "LHS_ID")
			},
			inverseJoinColumns = {
					@JoinColumn(name="RHS_ID")
	}
	)
	public SimpleEntity getOther() {
		return other;
	}

	public void setOther(SimpleEntity other) {
		this.other = other;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinTable(name = "ENTITY_ANOTHER")
	public BasicEntity getLazyOther() {
		return lazyOther;
	}

	public void setLazyOther(BasicEntity lazyOther) {
		this.lazyOther = lazyOther;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}
}
