/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.manytoone;
import java.io.Serializable;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Formula;

/**
 * @author Emmanuel Bernard
 */
@Entity
// "frame" is reserved in postgresplus
@Table(name = "FrameTable")
public class Frame implements Serializable {
	@Id
	@GeneratedValue
	private Long id;
	@OneToMany( mappedBy = "frame" )
	private Set<Lens> lenses;
	private String name;
	@Formula("lower(name)")
	private String lowerName;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<Lens> getLenses() {
		return lenses;
	}

	public void setLenses(Set<Lens> lenses) {
		this.lenses = lenses;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
