package org.hibernate.test.annotations.derivedidentities.e6.b;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class MedicalHistory {
	//all attributes map to relationship: AttributeOverride not allowed
	@EmbeddedId
	PersonId id;

	@MapsId
	@JoinColumns({
			@JoinColumn(name = "FK1", referencedColumnName = "firstName", nullable = false),
			@JoinColumn(name = "FK2", referencedColumnName = "lastName", nullable = false)
	})
	@OneToOne
	Person patient;
}