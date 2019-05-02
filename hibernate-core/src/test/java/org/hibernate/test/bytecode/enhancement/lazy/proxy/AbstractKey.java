/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "PP_DCKey")
public abstract class AbstractKey extends ModelEntity
		implements Serializable {

	@Column(name = "Name")
	String name;

	@OneToMany(targetEntity = RoleEntity.class, mappedBy = "key", fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	@LazyGroup("R")
	protected Set<RoleEntity> roles = new LinkedHashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	@LazyGroup("PR")
	@JoinColumn
	protected AbstractKey register;

	@OneToMany(targetEntity = AbstractKey.class, mappedBy = "register", fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	@LazyGroup("RK")
	protected Set<AbstractKey> keys = new LinkedHashSet();

	@ManyToOne(fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	@LazyGroup("PP")
	@JoinColumn
	protected AbstractKey parent;

	@OneToMany(targetEntity = AbstractKey.class, mappedBy = "parent", fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
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
