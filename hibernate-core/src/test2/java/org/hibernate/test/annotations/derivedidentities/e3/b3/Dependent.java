package org.hibernate.test.annotations.derivedidentities.e3.b3;

import javax.persistence.*;

@Entity
@Table(name="`Dependent`")
public class Dependent {

	@EmbeddedId
	DependentId id;

	@JoinColumns({
			@JoinColumn(name = "FIRSTNAME", referencedColumnName = "FIRSTNAME"),
			@JoinColumn(name = "LASTNAME", referencedColumnName = "lastName")
	})
	@MapsId("empPK")
	@ManyToOne
	Employee emp;
}
