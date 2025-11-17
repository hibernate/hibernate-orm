/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "PERSON")
public class Person {
	@Id
	@GeneratedValue
	private Long id;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinTable(name = "PERSON_PHONE",
			joinColumns = @JoinColumn(name = "PERSON_ID", foreignKey = @ForeignKey(name = "PERSON_ID_FK")),
			inverseJoinColumns = @JoinColumn(name = "PHONE_ID", foreignKey = @ForeignKey(name = "PHONE_ID_FK"))
	)
	private List<Phone> phones = new ArrayList<Phone>();
}
