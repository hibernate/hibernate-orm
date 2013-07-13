//$Id$
package org.hibernate.test.annotations.indexcoll;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class GenerationGroup {

	@Id
	@GeneratedValue
	private int id;

	private Generation generation;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Generation getGeneration() {
		return generation;
	}

	public void setGeneration(Generation generation) {
		this.generation = generation;
	}


}
