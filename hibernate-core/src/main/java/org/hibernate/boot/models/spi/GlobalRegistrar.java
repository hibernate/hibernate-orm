/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import org.hibernate.Remove;
import org.hibernate.models.spi.MemberDetails;

/**
 * @author Steve Ebersole
 */
@Remove
public interface GlobalRegistrar {
	void collectIdGenerators(MemberDetails memberDetails);
}
