package org.hibernate.test.annotations.derivedidentities.e3.b2;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

@Entity
public class Policy {
	@EmbeddedId
	PolicyId id;


	@MapsId("depPK")
	@ManyToOne
	Dependent dep;

}
