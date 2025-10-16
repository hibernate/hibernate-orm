/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.reflection;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(SocialSecurityNumber.class)
@DiscriminatorValue("Moral")
@SequenceGenerator(name = "seq")
@TableGenerator(name = "table")
public class SocialSecurityMoralAccount {
	public String number;
	public String countryCode;
}
