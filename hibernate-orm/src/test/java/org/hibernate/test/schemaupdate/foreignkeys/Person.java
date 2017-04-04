/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
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
