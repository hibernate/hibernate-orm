package org.hibernate.orm.test.annotations.sharedfk;

import jakarta.persistence.*;

@Entity
@Access(AccessType.FIELD)
@DiscriminatorValue("2")
public class ConcreteChild2 extends AbstractChild {
	@Basic(optional = false)
	@Column(name = "VALUE2")
	String value;
}