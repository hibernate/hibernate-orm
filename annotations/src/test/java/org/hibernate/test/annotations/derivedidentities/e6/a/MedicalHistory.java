package org.hibernate.test.annotations.derivedidentities.e6.a;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
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
@IdClass(PersonId.class)
public class MedicalHistory implements Serializable {
	@Id
	@JoinColumns({
			@JoinColumn(name = "FK1", referencedColumnName = "firstName"),
			@JoinColumn(name = "FK2", referencedColumnName = "lastName")
	})
	@OneToOne
	Person patient;
}
