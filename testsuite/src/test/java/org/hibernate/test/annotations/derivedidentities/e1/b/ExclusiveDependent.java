package org.hibernate.test.annotations.derivedidentities.e1.b;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class ExclusiveDependent {
	@EmbeddedId
	DependentId id;

	@JoinColumn(name = "FK")
	// id attribute mapped by join column default
	@MapsId("empPK")
	// maps empPK attribute of embedded id
	@OneToOne
	Employee emp;
}
