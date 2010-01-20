package org.hibernate.test.annotations.derivedidentities.e4.a;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class MedicalHistory {

	@Id String id; // overriding not allowed ... // default join column name is overridden @MapsId
	@Temporal(TemporalType.DATE)
	Date lastupdate;

	@JoinColumn(name = "FK")
	@MapsId
	@OneToOne
	Person patient;
}
