//$Id: GoalKeeper.java 14760 2008-06-11 07:33:15Z hardy.ferentschik $
package org.hibernate.test.annotations.id.sequences.entities;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class GoalKeeper extends Footballer {
	public GoalKeeper() {
	}

	public GoalKeeper(String firstname, String lastname, String club) {
		super( firstname, lastname, club );
	}
}
