//$Id$
package org.hibernate.test.annotations.manytoone;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

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
	@ForeignKey(name="BODY_COLOR_FK")
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
