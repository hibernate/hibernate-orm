/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf;

/**
 * @author Steve Ebersole
 */
public interface Name {
	String getFirstName();
	String getLastName();
}
