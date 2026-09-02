/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultGraphNode;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface EmbeddableResultGraphNode extends DomainResultGraphNode, FetchParent {
	@Override
	default NavigablePath getNavigablePath() {
		return null;
	}

	@Override
	EmbeddableValuedModelPart getReferencedMappingContainer();

	@Override
	EmbeddableMappingType getReferencedMappingType();
}
