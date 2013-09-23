/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.graphs;

import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Subgraph;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.graph.internal.advisor.EntityGraphBasedLoadPlanAdvisor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.loader.plan.internal.LoadPlanImpl;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.spi.LoadPlanAdvisor;

import org.junit.Ignore;
import org.junit.Test;

import org.hibernate.jpa.graph.internal.advisor.AdviceStyle;

import org.hibernate.testing.Skip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class BasicGraphLoadPlanAdviceTests extends BaseEntityManagerFunctionalTestCase {
	private static final String ENTIYT_NAME = "org.hibernate.jpa.test.graphs.BasicGraphLoadPlanAdviceTests$Entity1";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Entity1.class };
	}

	public void testNoAdvice() {
		EntityManager em = getOrCreateEntityManager();
	}

	private LoadPlan buildBasicLoadPlan() {
		return new LoadPlanImpl(
				new EntityReturn(
						sfi(),
						LockMode.NONE,
						ENTIYT_NAME
				)
		);
	}

	private SessionFactoryImplementor sfi() {
		return entityManagerFactory().unwrap( SessionFactoryImplementor.class );
	}

	@Test
	public void testBasicGraphBuilding() {
		EntityManager em = getOrCreateEntityManager();
		EntityGraph<Entity1> graphRoot = em.createEntityGraph( Entity1.class );
		assertNull( graphRoot.getName() );
		assertEquals( 0, graphRoot.getAttributeNodes().size() );

		LoadPlan loadPlan = buildBasicLoadPlan();

		LoadPlan advised = buildAdvisor( graphRoot, AdviceStyle.FETCH ).advise( loadPlan );
		assertNotSame( advised, loadPlan );
	}

	private LoadPlanAdvisor buildAdvisor(EntityGraph<Entity1> graphRoot, AdviceStyle adviceStyle) {
		return new EntityGraphBasedLoadPlanAdvisor( (EntityGraphImpl) graphRoot, adviceStyle );
	}

	@Test
	@Ignore
	public void testBasicSubgraphBuilding() {
		EntityManager em = getOrCreateEntityManager();
		EntityGraph<Entity1> graphRoot = em.createEntityGraph( Entity1.class );
		Subgraph<Entity1> parentGraph = graphRoot.addSubgraph( "parent" );
		Subgraph<Entity1> childGraph = graphRoot.addSubgraph( "children" );

		assertNull( graphRoot.getName() );
		assertEquals( 2, graphRoot.getAttributeNodes().size() );
		assertTrue(
				graphRoot.getAttributeNodes().get( 0 ).getSubgraphs().containsValue( parentGraph )
						|| graphRoot.getAttributeNodes().get( 0 ).getSubgraphs().containsValue( childGraph )
		);
		assertTrue(
				graphRoot.getAttributeNodes().get( 1 ).getSubgraphs().containsValue( parentGraph )
						|| graphRoot.getAttributeNodes().get( 1 ).getSubgraphs().containsValue( childGraph )
		);

		LoadPlan loadPlan = buildBasicLoadPlan();

		LoadPlan advised = buildAdvisor( graphRoot, AdviceStyle.FETCH ).advise( loadPlan );
		assertNotSame( advised, loadPlan );
	}

	@Entity( name = "Entity1" )
	public static class Entity1 {
		@Id
		public Integer id;
		public String name;
		@ManyToOne
		public Entity1 parent;
		@OneToMany( mappedBy = "parent" )
		public Set<Entity1> children;
	}
}
