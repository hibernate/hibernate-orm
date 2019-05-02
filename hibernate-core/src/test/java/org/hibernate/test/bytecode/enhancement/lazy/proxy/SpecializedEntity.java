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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;


@Entity(name="SpecializedEntity")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name="PP_PartnerZusatzKuerzelZR")
public class SpecializedEntity implements Serializable {

	@Id
	Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name="Value")
	String value;

	@ManyToOne(fetch=FetchType.LAZY)
	@LazyToOne(LazyToOneOption.PROXY)
	@LazyGroup("SpecializedKey")
	@JoinColumn
	protected SpecializedKey specializedKey = null;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public SpecializedKey getSpecializedKey() {
		return specializedKey;
	}

	public void setSpecializedKey(SpecializedKey specializedKey) {
			this.specializedKey = specializedKey;
	}
}
