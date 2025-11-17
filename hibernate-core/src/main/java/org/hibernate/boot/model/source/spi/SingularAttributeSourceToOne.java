/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.type.ForeignKeyDirection;

/**
 * Further contract for sources of singular associations ({@code one-to-one} and {@code many-to-one}).
 *
 * @author Steve Ebersole
 */
public interface SingularAttributeSourceToOne
		extends SingularAttributeSource,
			ForeignKeyContributingSource,
			FetchableAttributeSource,
			AssociationSource,
			CascadeStyleSource{

	String getReferencedEntityAttributeName();
	String getReferencedEntityName();
	ForeignKeyDirection getForeignKeyDirection();

	@Override
	FetchCharacteristicsSingularAssociation getFetchCharacteristics();

	boolean isUnique();

	Boolean isEmbedXml();
}
