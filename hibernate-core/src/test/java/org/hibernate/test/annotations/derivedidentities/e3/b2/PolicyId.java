package org.hibernate.test.annotations.derivedidentities.e3.b2;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class PolicyId implements Serializable {
	@Column(name = "`type`", length = 32)
	String type;
	DependentId depPK;
}
