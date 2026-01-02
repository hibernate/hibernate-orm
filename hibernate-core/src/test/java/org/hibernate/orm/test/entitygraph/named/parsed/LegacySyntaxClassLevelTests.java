/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.orm.test.entitygraph.named.parsed.entity.InvalidParsedGraphEntity;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@ServiceRegistry(settings = @Setting(name = AvailableSettings.GRAPH_PARSER_MODE, value = "legacy"))
public class LegacySyntaxClassLevelTests extends AbstractClassLevelTests {
	@Test
	@DomainModel(annotatedClasses = InvalidParsedGraphEntity.class)
	void testInvalidParsedGraph(DomainModelScope modelScope) {
		final MetadataImplementor domainModel = modelScope.getDomainModel();
		try {
			try (org.hibernate.SessionFactory sessionFactory = domainModel.buildSessionFactory()) {
				fail( "Expecting an exception" );
			}
			catch (InvalidGraphException expected) {
			}
		}
		catch (InvalidGraphException expected) {
		}
	}
}
