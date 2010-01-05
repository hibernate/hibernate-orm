package org.hibernate.test.annotations.embedded;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CollectionOfElements;

@Entity
public class WealthyPerson extends Person {

	@ElementCollection
//	@CollectionTable(name="XXXHOMES")
//	@AttributeOverrides({
//		@AttributeOverride(name="address1",
//								 column=@Column(name="HOME_STREET")),
//		@AttributeOverride(name="city",
//								 column=@Column(name="HOME_CITY")),
//		@AttributeOverride(name="country",
//								 column=@Column(name="HOME_COUNTRY"))
//	})
	protected Set<Address> vacationHomes = new HashSet<Address>();

	@CollectionOfElements
	protected Set<Address> legacyVacationHomes = new HashSet<Address>();

	@CollectionOfElements
	@CollectionTable(name = "WelPers_VacHomes")
	protected Set<Address> explicitVacationHomes = new HashSet<Address>();
}
