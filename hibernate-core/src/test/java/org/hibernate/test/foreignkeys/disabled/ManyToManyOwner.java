/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.foreignkeys.disabled;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SecondaryTable;

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
			name = "many_to_many"
	)
	public Set<ManyToManyTarget> getManyToMany() {
		return manyToMany;
	}

	public void setManyToMany(Set<ManyToManyTarget> manyToMany) {
		this.manyToMany = manyToMany;
	}
}
