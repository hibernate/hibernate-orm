package org.hibernate.test.annotations.derivedidentities.e3.b2;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name="`Policy`")
public class Policy {
	@EmbeddedId
	PolicyId id;


	@MapsId("depPK")
	@ManyToOne
	Dependent dep;

}
