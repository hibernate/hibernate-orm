/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.ejb3joincolumn;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Anton Wimmer
 * @author Steve Ebersole
 */
@Entity
//@Immutable
//@Cacheable
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
//@SuppressWarnings("serial")
public class Language {

	@Id
	@Access(AccessType.PROPERTY)
	private Long id = null;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

//	@Column(unique = true)
//	@Lob
//	@Type(type = "org.hibernate.type.TextType")
	private String name;

	@ManyToOne(optional = true)
	@JoinColumn
	private Language fallBack;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Language getFallBack() {
		return fallBack;
	}

	public void setFallBack(Language fallBack) {
		this.fallBack = fallBack;
	}
}
