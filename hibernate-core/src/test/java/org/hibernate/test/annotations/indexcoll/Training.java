//$Id$
package org.hibernate.test.annotations.indexcoll;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;

import org.hibernate.annotations.SortNatural;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Training {
	@Id @GeneratedValue private Long id;
	@SortNatural
	@MapKey(name="name") @ManyToMany SortedMap<String, Trainee> trainees = new TreeMap<String, Trainee>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public SortedMap<String, Trainee> getTrainees() {
		return trainees;
	}

	public void setTrainees(SortedMap<String, Trainee> trainees) {
		this.trainees = trainees;
	}
}
