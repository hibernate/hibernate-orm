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
package org.hibernate.test.fetchprofiles.join.selfReferencing;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;

/**
 * @author Steve Ebersole
 */
@Entity
@FetchProfiles(
		@FetchProfile(
				name = Employee.FETCH_PROFILE_TREE,
				fetchOverrides = {
						@FetchProfile.FetchOverride(entity = Employee.class, association = "manager", mode = FetchMode.JOIN),
						@FetchProfile.FetchOverride(entity = Employee.class, association = "minions", mode = FetchMode.JOIN)
				}
		)
)
public class Employee {
	public final static String FETCH_PROFILE_TREE = "locationTree";

	private Long id;
	private Employee manager;
	private Collection<Employee> minions = new ArrayList<Employee>();

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "manager_id")
	public Employee getManager() {
		return manager;
	}

	public void setManager(Employee manager) {
		this.manager = manager;
	}

	@OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
	public Collection<Employee> getMinions() {
		return minions;
	}

	public void setMinions(Collection<Employee> minions) {
		this.minions = minions;
	}
}
