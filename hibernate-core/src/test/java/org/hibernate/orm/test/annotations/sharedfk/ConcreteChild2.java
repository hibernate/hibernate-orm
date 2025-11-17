/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.sharedfk;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("2")
public class ConcreteChild2 extends AbstractChild {
	@Basic(optional = false)
	@Column(name = "VALUE2")
	String value;
}
