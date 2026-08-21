/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.graph;

import org.hibernate.cfg.GraphParserSettings;
import org.hibernate.graph.EntityGraphs;
import org.hibernate.orm.test.graph.entity.GraphParsingTestEntity;

import org.hibernate.orm.test.graph.entity.GraphParsingTestSubentity;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;

import jakarta.persistence.EntityGraph;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


@Jpa(
		annotatedClasses = {
				GraphParsingTestEntity.class,
				GraphParsingTestSubentity.class
		},
		integrationSettings = {
				@Setting(name = GraphParserSettings.GRAPH_PARSER_MODE, value = "modern")
		}
)
public class EntityGraphsTest extends AbstractEntityGraphTest {

	@Test
	public void testEqualLinksWithSubclassesEqual(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph(
				"linkToOne(name), linkToOne:GraphParsingTestSubentity(description)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph(
				"linkToOne:GraphParsingTestSubentity(description), linkToOne(name)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isTrue();
	}

	@Test
	public void testDifferentLinksEqual3(EntityManagerFactoryScope scope) {
		EntityGraph<GraphParsingTestEntity> a = parseGraph(
				"linkToOne(name), linkToOne:GraphParsingTestSubentity(description)", scope );
		EntityGraph<GraphParsingTestEntity> b = parseGraph( "linkToOne(name, description)", scope );
		assertThat( EntityGraphs.areEqual( a, b ) ).isFalse();
	}


}
