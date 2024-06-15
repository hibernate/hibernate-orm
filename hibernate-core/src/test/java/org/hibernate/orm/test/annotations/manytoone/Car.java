/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.orm.test.annotations.manytoone;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Many to one sample using default mapping values
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Car {
	private Integer id;
	private Color bodyColor;
	private Parent owner;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(foreignKey = @ForeignKey(name="BODY_COLOR_FK"))
	public Color getBodyColor() {
		return bodyColor;
	}

	public void setBodyColor(Color bodyColor) {
		this.bodyColor = bodyColor;
	}

	@ManyToOne
	public Parent getOwner() {
		return owner;
	}

	public void setOwner(Parent owner) {
		this.owner = owner;
	}
}
