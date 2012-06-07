//$Id$
package org.hibernate.test.annotations.polymorphism;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.annotations.PolymorphismType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
@org.hibernate.annotations.Entity(polymorphism = PolymorphismType.EXPLICIT)
public class Car extends MovingThing {
	private Integer id;
	private String model;

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@Id @GeneratedValue(strategy = GenerationType.TABLE )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
