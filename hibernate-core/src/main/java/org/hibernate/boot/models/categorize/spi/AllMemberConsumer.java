/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.models.spi.MemberDetails;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface AllMemberConsumer {
	void acceptMember(MemberDetails memberDetails);
}
