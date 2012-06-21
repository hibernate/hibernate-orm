//$Id$
package org.hibernate.test.annotations.array;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.IndexColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Contest {
	private int id;
	private Competitor[] results;
	private Month[] heldIn;

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@OrderColumn(name = "pos")
	public Competitor[] getResults() {
		return results;
	}

	public void setResults(Competitor[] results) {
		this.results = results;
	}
	
	@ElementCollection
	@IndexColumn(name = "pos", base=1) //legacy + base
	public Month[] getHeldIn() {
		return heldIn;
	}

	public void setHeldIn(Month[] heldIn) {
		this.heldIn = heldIn;
	}
	
	public enum Month {
		January, February, March, April, May, June, July, August, September, October, November, December;
	}; 
}
