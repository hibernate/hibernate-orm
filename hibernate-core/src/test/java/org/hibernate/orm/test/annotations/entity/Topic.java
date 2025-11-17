/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * @author Sharath Reddy
 */
@FilterDef(name="byState", defaultCondition=":state = state",
		parameters=@ParamDef(name="state",type=String.class))
@Entity
public class Topic {

	@Id @GeneratedValue
	private int id;

	private String name;

	@OneToMany(mappedBy="topic", cascade=CascadeType.ALL)
	@Filter(name="byState", condition=":state = state")
	private Set<Narrative> narratives = new HashSet<>();


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<Narrative> getNarratives() {
		return narratives;
	}

	public void addNarrative(Narrative val) {
		narratives.add(val);
		val.setTopic(this);
	}

}
