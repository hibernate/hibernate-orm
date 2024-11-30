/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class SimplePerson {
	@Id
	String ssn;
}
