/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.parser;

import java.util.List;

import org.hibernate.UnknownEntityTypeException;
import org.hibernate.cfg.GraphParserSettings;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Jpa(
		annotatedClasses = {
				GraphParsingTestEntity.class,
				GraphParsingTestSubEntity.class
		},
		integrationSettings = {
				@Setting(name = GraphParserSettings.GRAPH_PARSER_MODE, value = "modern")
		}
)
public class EntityGraphParserTest extends AbstractEntityGraphParserTest {


	@Test
	public void testLinkSubtypeParsingWithNewSyntax(EntityManagerFactoryScope scope) {
		RootGraphImplementor<GraphParsingTestEntity> graph = parseGraph(
				"linkToOne(name, description), linkToOne:GraphParsingTestSubEntity(sub)", scope );
		assertThat( graph ).isNotNull();

		List<? extends AttributeNodeImplementor<?, ?, ?>> attrs = graph.getAttributeNodeList();
		assertThat( attrs ).isNotNull();
		assertThat( attrs ).hasSize( 1 );

		AttributeNodeImplementor<?, ?, ?> linkToOneNode = attrs.get( 0 );
		assertThat( linkToOneNode ).isNotNull();
		assertThat( linkToOneNode.getAttributeName() ).isEqualTo( "linkToOne" );

		AssertionHelper.assertNullOrEmpty( linkToOneNode.getKeySubgraphs() );

		final SubGraphImplementor<?> subgraph = linkToOneNode.getSubGraphs().get( GraphParsingTestSubEntity.class );
		assertThat( subgraph ).isNotNull();

		AssertionHelper.assertBasicAttributes( subgraph, "sub" );
	}

	@Test
	public void testSubtypeAndTwoBasicAttributesParsing(EntityManagerFactoryScope scope) {
		var graph = parseGraph( "name, :GraphParsingTestSubEntity(sub), description", scope );
		assertThat( graph ).isNotNull();

		AssertionHelper.assertBasicAttributes( graph, "name", "description" );

		var treatedSubgraphs = graph.getTreatedSubgraphs();
		assertThat( treatedSubgraphs ).hasSize( 1 );


		var subEntityGraph = treatedSubgraphs.get( GraphParsingTestSubEntity.class );
		var subEntityGraphAttributes = subEntityGraph.getAttributeNodes();
		assertThat( subEntityGraphAttributes ).isNotNull();
		assertThat( subEntityGraphAttributes ).hasSize( 1 );

		var subEntityGraphAttributeNode = subEntityGraphAttributes.get( 0 );
		assertThat( subEntityGraphAttributeNode ).isNotNull();
		assertThat( subEntityGraphAttributeNode.getAttributeName() ).isEqualTo( "sub" );
	}

	@Test
	public void testSubtypeParsing(EntityManagerFactoryScope scope) {
		var graph = parseGraph( ":GraphParsingTestSubEntity(sub)", scope );
		assertThat( graph ).isNotNull();

		var treatedSubgraphs = graph.getTreatedSubgraphs();
		assertThat( treatedSubgraphs ).hasSize( 1 );

		var subEntityGraph = treatedSubgraphs.get( GraphParsingTestSubEntity.class );
		var subEntityGraphAttributes = subEntityGraph.getAttributeNodes();

		assertThat( subEntityGraphAttributes ).isNotNull();
		assertThat( subEntityGraphAttributes ).hasSize( 1 );

		var attributeNode = subEntityGraphAttributes.get( 0 );
		assertThat( attributeNode ).isNotNull();
		assertThat( attributeNode.getAttributeName() ).isEqualTo( "sub" );
	}


	@Test
	public void testWithSubTypeThatNotExist(EntityManagerFactoryScope scope) {
		assertThrows(
				UnknownEntityTypeException.class,
				() -> parseGraph( ":SubTypeThatNotExist(sub)", scope )
		);

	}
}
