/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.circulartypevariable;

import java.util.Set;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

@MappedSuperclass
@Access( AccessType.PROPERTY )
public class User<
		TUser extends User<TUser,TRoleAccess>,
		TRoleAccess extends RoleAccess<TRoleAccess,TUser>> {
	private Long id;
	private Set<TRoleAccess> roleAccesses;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@OneToMany
	public Set<TRoleAccess> getRoleAccesses() {
		return roleAccesses;
	}

	public void setRoleAccesses(Set<TRoleAccess> roleAccesses) {
		this.roleAccesses = roleAccesses;
	}
}
