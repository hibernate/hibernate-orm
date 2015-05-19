/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.naturalid;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

/**
 * @author Guenther Demetz
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NaturalIdCache
public class A {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private long oid;

	@Version
	private int version;

	@Column
	@NaturalId(mutable = false)
	private String name;

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@org.hibernate.annotations.OptimisticLock(excluded = true)
	@javax.persistence.OneToMany(mappedBy = "a")
	private Set<D> ds = new HashSet<D>();

	@javax.persistence.OneToOne
	private D singleD = null;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<D> getDs() {
		return ds;
	}

	public void setDs(Set<D> ds) {
		this.ds = ds;
	}

	public D getSingleD() {
		return singleD;
	}

	public void setSingleD(D singleD) {
		this.singleD = singleD;
	}

}
