/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e6.b;
import java.io.Serializable;
import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class PersonId implements Serializable {
	String firstName;
	String lastName;
}
