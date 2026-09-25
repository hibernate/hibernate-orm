package org.hibernate.processor.test.inheritance.unmappedclassinhierarchy;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;

@Entity
@Access(AccessType.FIELD)
public class SubB extends NormalExtendsMapped {
	protected String street;

	public String getStreet() {
		return street;
	}
}
