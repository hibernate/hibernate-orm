/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import org.hibernate.models.spi.MemberDetails;

/**
 * @author Steve Ebersole
 */
public interface GlobalRegistrar {
	void collectIdGenerators(MemberDetails memberDetails);
}
