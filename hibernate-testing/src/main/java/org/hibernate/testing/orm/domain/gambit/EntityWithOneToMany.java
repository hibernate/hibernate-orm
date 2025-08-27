/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * @author Andrea Boriero
 */
@Entity
@GenericGenerator(name="increment", strategy = "increment")
public class EntityWithOneToMany {
	private Integer id;

	// alphabetical
	private String name;
	private Set<SimpleEntity> others = new HashSet<>(  );
	private List<SimpleEntity> othersIdentifierBag = new ArrayList<>(  );
	private Integer someInteger;

	public EntityWithOneToMany() {
	}

	public EntityWithOneToMany(Integer id, String name, Integer someInteger) {
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

	@OneToMany(fetch = FetchType.LAZY)
	public Set<SimpleEntity> getOthers() {
		return others;
	}

	public void setOthers(Set<SimpleEntity> others) {
		this.others = others;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}

	public void addOther(SimpleEntity other) {
		others.add( other );
	}

	@OneToMany
	@CollectionTable(name = "idbag")
	@CollectionId( column = @Column(name = "BAG_ID"), generator = "increment" )
	@CollectionIdJdbcTypeCode( Types.BIGINT )
	public List<SimpleEntity> getOthersIdentifierBag() {
		return othersIdentifierBag;
	}

	public void setOthersIdentifierBag(List<SimpleEntity> othersIdentifierBag) {
		this.othersIdentifierBag = othersIdentifierBag;
	}
}
