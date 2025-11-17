/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.LazyGroup;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "PP_DCKey")
public abstract class AbstractKey extends ModelEntity
		implements Serializable {

	@Column(name = "Name")
	String name;

	@OneToMany(targetEntity = RoleEntity.class, mappedBy = "key", fetch = FetchType.LAZY)
	@LazyGroup("R")
	protected Set<RoleEntity> roles = new LinkedHashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@LazyGroup("PR")
	@JoinColumn
	protected AbstractKey register;

	@OneToMany(targetEntity = AbstractKey.class, mappedBy = "register", fetch = FetchType.LAZY)
	@LazyGroup("RK")
	protected Set<AbstractKey> keys = new LinkedHashSet();

	@ManyToOne(fetch = FetchType.LAZY)
	@LazyGroup("PP")
	@JoinColumn
	protected AbstractKey parent;

	@OneToMany(targetEntity = AbstractKey.class, mappedBy = "parent", fetch = FetchType.LAZY)
	@LazyGroup("PK")
	protected Set<AbstractKey> otherKeys = new LinkedHashSet();


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<RoleEntity> getRoles() {
		return roles;
	}

	public void setRoles(Set<RoleEntity> role) {
		this.roles = role;
	}

	public void addRole(RoleEntity role) {
		this.roles.add( role );
	}

	public AbstractKey getRegister() {
		return register;
	}

	public void setRegister(AbstractKey register) {
		this.register = register;
	}

	public Set<AbstractKey> getKeys() {
		return keys;
	}

	public void setKeys(Set<AbstractKey> keys) {
		this.keys = keys;
	}

	public void addRegisterKey(AbstractKey registerKey) {
		keys.add( registerKey );
	}

	public AbstractKey getParent() {
		return parent;
	}

	public void setParent(AbstractKey parent) {
		this.parent = parent;
	}

	public Set<AbstractKey> getOtherKeys() {
		return otherKeys;
	}

	public void setOtherKeys(Set<AbstractKey> otherKeys) {
		this.otherKeys = otherKeys;
	}

	public void addPanelKey(AbstractKey panelKey) {
		this.otherKeys.add( panelKey );
	}

}
