/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy.ejb3joincolumn;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
