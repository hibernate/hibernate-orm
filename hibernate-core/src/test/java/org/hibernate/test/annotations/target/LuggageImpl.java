/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.target;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Target;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class LuggageImpl implements Luggage {
	private Long id;
	private double height;
	private double width;
	private Owner owner;

	@Embedded
	@Target(OwnerImpl.class)
	@Override
	public Owner getOwner() {
		return owner;
	}

	@Override
	public void setOwner(Owner owner) {
		this.owner = owner;
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public double getHeight() {
		return height;
	}

	@Override
	public void setHeight(double height) {
		this.height = height;
	}

	@Override
	public double getWidth() {
		return width;
	}

	@Override
	public void setWidth(double width) {
		this.width = width;
	}
}
