/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.foreignkeys.disabled;

import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 * @author Steve Ebersole
 */
@Entity
public class ManyToManyOwner {
	public Integer id;
	public String name;
	private Set<ManyToManyTarget> manyToMany;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// NOTE : we currently only pick up the JoinTable#foreignKey / JoinTable#inverseForeignKey
	// 		not the JoinColumn#foreignKey reference on the JoinTable#joinColumns /JoinTable#inverseJoinColumns

	@ManyToMany(
			fetch = FetchType.LAZY,
			cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinTable(
			foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT),
			joinColumns = {
					@JoinColumn(
							unique = false,
							nullable = true,
							insertable = true,
							updatable = true,
							foreignKey = @ForeignKey( name = "none" ),
							name = "m2m_owner_id"
					)
			},
			inverseJoinColumns = {
					@JoinColumn(
							unique = false,
							nullable = true,
							insertable = true,
							updatable = true,
							foreignKey = @ForeignKey( name = "none", value = ConstraintMode.NO_CONSTRAINT ),
							name = "m2m_target_id"
					)
			},
			inverseForeignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT),
			name = "Many_To_Many"
	)
	public Set<ManyToManyTarget> getManyToMany() {
		return manyToMany;
	}

	public void setManyToMany(Set<ManyToManyTarget> manyToMany) {
		this.manyToMany = manyToMany;
	}
}
