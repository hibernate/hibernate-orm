package org.hibernate.test.annotations.derivedidentities.e3.b2;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

@Entity
public class Dependent {

	@EmbeddedId
	DependentId id;

	@MapsId("empPK")
	@ManyToOne
	Employee emp;
}
