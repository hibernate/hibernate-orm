package org.hibernate.test.annotations.derivedidentities.e3.b3;

import javax.persistence.*;

@Entity
public class Policy {
	@EmbeddedId
	PolicyId id;

	@JoinColumns({
			@JoinColumn(name = "FIRSTNAME", referencedColumnName = "FIRSTNAME", nullable = false),
			@JoinColumn(name = "LASTNAME", referencedColumnName = "lastName", nullable = false),
			@JoinColumn(name = "NAME", referencedColumnName = "Name", nullable = false)
	})
	@MapsId("depPK")
	@ManyToOne
	Dependent dep;

}
