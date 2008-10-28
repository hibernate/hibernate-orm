package org.hibernate.test.annotations.naturalid;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;

@Entity
/**
 * Test case for NaturalId annotation - ANN-750
 * 
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
class NaturalIdOnManyToOne {

    @Id
    @GeneratedValue
    int id;

    @NaturalId
    @ManyToOne
    Citizen citizen;
    
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Citizen getCitizen() {
		return citizen;
	}

	public void setCitizen(Citizen citizen) {
		this.citizen = citizen;
	}
} 
