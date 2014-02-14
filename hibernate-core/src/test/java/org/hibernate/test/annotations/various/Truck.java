//$Id$
package org.hibernate.test.annotations.various;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(indexes = {
		@Index(name = "weigth_idx", columnList = "weight"),
		@Index(name = "agreement_idx", columnList = "agreement_id")})
public class Truck extends Vehicule {
	private int weight;

	@ManyToOne
	@JoinColumn(name = "agreement_id")
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
