/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.mutable.cached;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

/**
 * @author Guenther Demetz
 * Same as Another.class, excpect that here also the entity class itself is configured for shared cache
 */
@Entity
@NaturalIdCache
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AllCached {
	private Integer id;
	private String name;

	public AllCached() {
	}

	public AllCached(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@NaturalId(mutable = true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
