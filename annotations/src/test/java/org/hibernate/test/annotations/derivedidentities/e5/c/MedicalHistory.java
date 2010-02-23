package org.hibernate.test.annotations.derivedidentities.e5.c;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;


/**
 * @author Emmanuel Bernard
 */
@Entity
public class MedicalHistory implements Serializable {
	@Id
	Integer id;

	@MapsId
	@JoinColumn(name = "patient_id")
	@OneToOne
	Person patient;
}