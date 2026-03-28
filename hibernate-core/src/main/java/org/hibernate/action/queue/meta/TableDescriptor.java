/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.hibernate.sql.model.TableMapping;


/**
 * @author Steve Ebersole
 */
public interface TableDescriptor {
	String name();

	boolean isOptional();

	TableKeyDescriptor keyDescriptor();

	boolean cascadeDeleteEnabled();

	TableMapping.MutationDetails insertDetails();

	TableMapping.MutationDetails updateDetails();

	TableMapping.MutationDetails deleteDetails();

	int getRelativePosition();
}
