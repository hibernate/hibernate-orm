/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.inheritance.deep;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * A base entity that defines an id attribute. Default access level should be
 * resolved from this class instead of continuing to {@link PersistenceBase}.
 *
 * @author Igor Vaynberg
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "planetype", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("Plane")
public class Plane extends PersistenceBase {
	@GeneratedValue
	@Id
	private Long id;
}
