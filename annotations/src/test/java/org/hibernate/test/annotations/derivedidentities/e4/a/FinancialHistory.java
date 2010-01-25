package org.hibernate.test.annotations.derivedidentities.e4.a;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


/**
 * @author Emmanuel Bernard
 */
@Entity
public class FinancialHistory implements Serializable {
	
	@Temporal(TemporalType.DATE)
	Date lastupdate;

	@Id
	//@JoinColumn(name = "FK")
	@ManyToOne
	Person patient;

}