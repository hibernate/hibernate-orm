//$Id$
package org.hibernate.test.annotations.inheritance.singletable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorValue("1")
public class Funk extends Music {
	private int starred;

	public int getStarred() {
		return starred;
	}

	public void setStarred(int starred) {
		this.starred = starred;
	}
}
