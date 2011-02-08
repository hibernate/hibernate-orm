// $Id$
package org.hibernate.test.annotations.fkcircularity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

/**
 * Test entities ANN-730.
 * 
 * @author Hardy Ferentschik
 * 
 */
@Entity
@Table(name = "class_a")
@Inheritance(strategy = InheritanceType.JOINED)
public class ClassA {

	private int id;

	@Id
	@Column(name = "id")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
