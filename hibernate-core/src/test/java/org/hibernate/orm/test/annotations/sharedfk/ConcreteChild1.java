package org.hibernate.orm.test.annotations.sharedfk;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@Access(AccessType.FIELD)
@DiscriminatorValue("1")
public class ConcreteChild1 extends AbstractChild {
	@Basic(optional = false)
	@Column(name = "VALUE1")
	String value;
}
