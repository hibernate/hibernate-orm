/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.parser;

import java.util.List;

import org.hibernate.cfg.GraphParserSettings;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				GraphParsingTestEntity.class,
				GraphParsingTestSubEntity.class
		},
		integrationSettings = {
				@Setting(name = GraphParserSettings.GRAPH_PARSER_MODE, value = "legacy")
		}
)
public class LegacySyntaxEntityGraphParserTest extends AbstractEntityGraphParserTest {

	@Test
	public void testLinkSubtypeParsing(EntityManagerFactoryScope scope) {
		RootGraphImplementor<GraphParsingTestEntity> graph = parseGraph(
				"linkToOne(name, description), linkToOne(GraphParsingTestSubEntity: sub)", scope );
		assertThat( graph ).isNotNull();

		List<? extends AttributeNodeImplementor<?, ?, ?>> attrs = graph.getAttributeNodeList();
		assertThat( attrs ).isNotNull();
		assertThat( attrs.size() ).isEqualTo( 1 );

		AttributeNodeImplementor<?, ?, ?> linkToOneNode = attrs.get( 0 );
		assertThat( linkToOneNode ).isNotNull();
		assertThat( linkToOneNode.getAttributeName() ).isEqualTo( "linkToOne" );

		AssertionHelper.assertNullOrEmpty( linkToOneNode.getKeySubgraphs() );

		final SubGraphImplementor<?> subgraph = linkToOneNode.getSubGraphs().get( GraphParsingTestSubEntity.class );
		assertThat( subgraph ).isNotNull();

		AssertionHelper.assertBasicAttributes( subgraph, "sub" );
	}

}
