package org.hibernate.test.annotations.derivedidentities.e3.b3;

import javax.persistence.*;

@Entity
public class Dependent {

	@EmbeddedId
	DependentId id;

	@JoinColumns({
			@JoinColumn(name = "FIRSTNAME", referencedColumnName = "FIRSTNAME", nullable = false),
			@JoinColumn(name = "LASTNAME", referencedColumnName = "lastName", nullable = false)
	})
	@MapsId("empPK")
	@ManyToOne
	Employee emp;
}
