/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.discriminator;

/**
 * @author Steve Ebersole
 */
//tag::associations-any-example[]
public interface Payment {
	// ...
//end::associations-any-example[]
	Double getAmount();
//tag::associations-any-example[]
}
//end::associations-any-example[]
