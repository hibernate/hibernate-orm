/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;

@Entity
public class WealthyPerson extends Person {

	@ElementCollection
	protected Set<Address> vacationHomes = new HashSet<Address>();

	@ElementCollection
	@CollectionTable(name = "WelPers_LegacyVacHomes")
	protected Set<Address> legacyVacationHomes = new HashSet<Address>();

	@ElementCollection
	@CollectionTable(name = "WelPers_VacHomes", indexes = @Index( columnList = "countryName, type_id"))
	protected Set<Address> explicitVacationHomes = new HashSet<Address>();
}
