package org.hibernate.test.annotations.derivedidentities.e4.a;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class MedicalHistory {
	@Id
	String id; // overriding not allowed ... // default join column name is overridden @MapsId
	@JoinColumn(name = "FK")
	@MapsId
	@OneToOne
	Person patient;
}
