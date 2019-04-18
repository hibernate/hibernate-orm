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
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity(name = "RoleEntity")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "PP_DCRolleKey")
public class RoleEntity extends ModelEntity implements Serializable {

	@Basic
	Short value;

	@ManyToOne(fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.PROXY)
	@LazyGroup("Key")
	@JoinColumn
	protected AbstractKey key = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.PROXY)
	@LazyGroup("Key")
	@JoinColumn
	protected SpecializedKey specializedKey = null;

	public Short getValue() {
		return value;
	}

	public void setvalue(Short value) {
		this.value = value;
	}

	public AbstractKey getKey() {
		return key;
	}

	public void setKey(AbstractKey key) {
		this.key = key;
	}

	public SpecializedKey getSpecializedKey() {
		return specializedKey;
	}

	public void setSpecializedKey(SpecializedKey specializedKey) {
		this.specializedKey = specializedKey;
	}
}
