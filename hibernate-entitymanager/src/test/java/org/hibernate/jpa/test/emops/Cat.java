//$Id$
package org.hibernate.jpa.test.emops;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance( strategy = InheritanceType.JOINED )
public class Cat extends Pet {
	int lives;

	public int getLives() {
		return lives;
	}

	public void setLives(int lives) {
		this.lives = lives;
	}
}