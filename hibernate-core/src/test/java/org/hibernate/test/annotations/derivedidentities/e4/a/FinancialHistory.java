package org.hibernate.test.annotations.derivedidentities.e4.a;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


/**
 * @author Emmanuel Bernard
 */
@Entity
public class FinancialHistory implements Serializable {
	@Id
	//@JoinColumn(name = "FK")
	@ManyToOne
	Person patient;

	@Temporal(TemporalType.DATE)
	Date lastUpdate;

	public FinancialHistory() {
	}

	public FinancialHistory(Person patient) {
		this.patient = patient;
	}
}
