/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.identity.joinedSubClass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

import static jakarta.persistence.GenerationType.IDENTITY;
import static jakarta.persistence.InheritanceType.JOINED;

/**
 * @author Andrey Vlasov
 * @author Steve Ebersole
 */
@Entity
@Inheritance(strategy = JOINED)
public class Super {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	@Column(name="`value`")
	private Long value;
}
