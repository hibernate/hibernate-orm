package org.hibernate.orm.test.metamodel.generics.embeddedid;

import jakarta.persistence.Entity;

@Entity
public class PersonEntity extends BaseEntity<PersonId> {
	public PersonEntity() {
	}

	public PersonEntity(PersonId id, String name) {
		super( id, name );
	}
}
