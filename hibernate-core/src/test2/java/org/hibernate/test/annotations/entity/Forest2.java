/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.Polymorphism;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Type;

/**
 * Mapping following lines of {@link Forest}, but using the replacements for the now deprecated
 * {@link org.hibernate.annotations.Entity} annotation.
 *
 * @author Steve Ebersole
 */
@Entity
@DynamicInsert
@DynamicUpdate
@SelectBeforeUpdate
@OptimisticLocking( type = OptimisticLockType.ALL )
@Polymorphism( type = PolymorphismType.EXPLICIT )
public class Forest2 {
	private Integer id;
	private String name;
	private String longDescription;

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

	@OptimisticLock(excluded=true)
	@Type(type = "text")
	public String getLongDescription() {
		return longDescription;
	}

	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}
}
