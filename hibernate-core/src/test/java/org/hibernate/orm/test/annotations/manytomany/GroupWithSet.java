/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.manytomany;

import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "tbl_group")
public class GroupWithSet {
	private Integer id;
	private Set<Permission> permissions;

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToMany(cascade = CascadeType.PERSIST)
	@OrderBy("expirationDate")
	@SQLRestriction("1=1")
	@SQLJoinTableRestriction("2=2")
	@Filter(name="Groupfilter", condition = "3=3")
	@FilterJoinTable(name="Groupfilter", condition = "4=4")
	public Set<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}
}
