/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

package org.hibernate.test.cache;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Cacheable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="`CompanyManyToMany`")
@Cacheable
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL, region="CompanyManyToMany")
public class CompanyManyToMany {
	@Id
	int id;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, targetEntity=UserManyToMany.class, mappedBy="companies")
    // @JoinTable(name = "company_user", joinColumns = { @JoinColumn(nullable = false, name = "company2_id") }, inverseJoinColumns = { @JoinColumn(
    //         nullable = false, name = "user2_id") })
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region="usersManyToMany")
	Set<UserManyToMany> users = new HashSet<UserManyToMany>();

	public CompanyManyToMany() {
	}

	public CompanyManyToMany(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<UserManyToMany> getUsers() {
		return users;
	}

	public void setUsers(Set<UserManyToMany> users) {
		this.users = users;
	}
}
