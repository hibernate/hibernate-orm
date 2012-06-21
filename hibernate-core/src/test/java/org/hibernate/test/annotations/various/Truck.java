//$Id$
package org.hibernate.test.annotations.various;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Index;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Truck extends Vehicule {
	@Index(name = "weigth_idx")
	private int weight;

	@ManyToOne
	@JoinColumn(name = "agreement_id")
	@Index(name = "agreement_idx")
	private ProfessionalAgreement agreement;

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public ProfessionalAgreement getAgreement() {
		return agreement;
	}

	public void setAgreement(ProfessionalAgreement agreement) {
		this.agreement = agreement;
	}

}
