package org.hibernate.test.annotations.embedded;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;

@Entity
public class WealthyPerson extends Person {

	@ElementCollection
	protected Set<Address> vacationHomes = new HashSet<Address>();

	@ElementCollection
	protected Set<Address> legacyVacationHomes = new HashSet<Address>();

	@ElementCollection
	@CollectionTable(name = "WelPers_VacHomes")
	protected Set<Address> explicitVacationHomes = new HashSet<Address>();
}
