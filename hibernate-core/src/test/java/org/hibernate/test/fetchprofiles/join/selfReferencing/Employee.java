/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
