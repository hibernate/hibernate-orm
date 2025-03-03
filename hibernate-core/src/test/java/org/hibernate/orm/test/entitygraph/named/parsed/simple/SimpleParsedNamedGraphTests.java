/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.simple;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.NamedEntityGraph;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class SimpleParsedNamedGraphTests {
	@Test
	@DomainModel(annotatedClasses = SimpleParsedNamedGraphTests.TestEntity.class)
	@SessionFactory(exportSchema = false)
	@NotImplementedYet( reason = "Support for parsed NamedEntityGraph is not implemented" )
	void checkGraphs(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		{
			final RootGraphImplementor<?> graph = sessionFactory.findEntityGraphByName( "test-id-name" );
			assertThat( graph ).isNotNull();
			final List<? extends AttributeNodeImplementor<?,?,?>> attributeNodes = graph.getAttributeNodeList();
			assertThat( attributeNodes ).hasSize( 2 );
		}

		{
			final RootGraphImplementor<?> graph = sessionFactory.findEntityGraphByName( "test-id-name-parent" );
			assertThat( graph ).isNotNull();
			final List<? extends AttributeNodeImplementor<?,?,?>> attributeNodes = graph.getAttributeNodeList();
			assertThat( attributeNodes ).hasSize( 3 );
		}
	}

	@NamedEntityGraph( name = "test-id-name", graph = "(id, name)" )
	@NamedEntityGraph( name = "test-id-name-parent", graph = "(id, name, parent)" )
	@Entity
	public static class TestEntity {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		public TestEntity parent;
		@OneToMany( mappedBy = "parent" )
		public Set<TestEntity> children;
	}
}
