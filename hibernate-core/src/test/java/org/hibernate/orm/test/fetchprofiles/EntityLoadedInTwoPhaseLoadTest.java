/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchprofiles;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.StatisticsSettings.GENERATE_STATISTICS;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-12297")
@ServiceRegistry(
		settings = @Setting( name = GENERATE_STATISTICS, value = "true" )
)
@DomainModel( annotatedClasses = {
		EntityLoadedInTwoPhaseLoadTest.StartNode.class,
		EntityLoadedInTwoPhaseLoadTest.MiddleNode.class,
		EntityLoadedInTwoPhaseLoadTest.TerminalNode.class,
		EntityLoadedInTwoPhaseLoadTest.Branch1Node.class,
		EntityLoadedInTwoPhaseLoadTest.Branch2Node.class
} )
@SessionFactory
public class EntityLoadedInTwoPhaseLoadTest {
	static final String FETCH_PROFILE_NAME = "fp1";

	@Test
	public void testIfAllRelationsAreInitialized(SessionFactoryScope sessions) {
		final StatisticsImplementor statistics = sessions.getSessionFactory().getStatistics();
		statistics.clear();

		final StartNode startNode = sessions.fromTransaction( (session) -> {
			session.enableFetchProfile( FETCH_PROFILE_NAME );
			return session.find( StartNode.class, 1 );
		} );

		// should have loaded all the data
		assertThat( statistics.getEntityLoadCount() ).isEqualTo( 4 );
		// should have loaded it in one query (join fetch)
		assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 1 );

		try {
			// access the data which was supposed to have been fetched
			assertThat( startNode.branch2Node.middleNode.terminalNode.name ).isNotNull();
		}
		catch (LazyInitializationException e) {
			fail( "Everything should be initialized" );
		}
	}

	@BeforeEach
	void createTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			TerminalNode terminalNode = new TerminalNode( 1, "foo" );
			MiddleNode middleNode = new MiddleNode( 1, terminalNode );
			Branch2Node branch2Node = new Branch2Node( 1, middleNode );
			StartNode startNode = new StartNode( 1, null, branch2Node );

			session.persist( startNode );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope sessions) {
		sessions.dropData();
	}

	@Entity
	@Table(name="start_node")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = StartNode.class, association = "branch1Node"),
			@FetchProfile.FetchOverride(entity = StartNode.class, association = "branch2Node")
	})
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class StartNode {
		@Id
		private Integer id;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Branch1Node branch1Node;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Branch2Node branch2Node;

		public StartNode() {
		}

		public StartNode(Integer id, Branch1Node branch1Node, Branch2Node branch2Node) {
			this.id = id;
			this.branch1Node = branch1Node;
			this.branch2Node = branch2Node;
		}
	}

	@Entity(name = "Via1Entity")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = Branch1Node.class, association = "middleNode")
	})
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class Branch1Node {
		@Id
		private Integer id;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private MiddleNode middleNode;

		public Branch1Node() {
		}

		public Branch1Node(Integer id, MiddleNode middleNode) {
			this.id = id;
			this.middleNode = middleNode;
		}
	}

	@Entity(name = "Via2Entity")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = Branch2Node.class, association = "middleNode")
	})
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class Branch2Node {
		@Id
		private Integer id;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private MiddleNode middleNode;

		public Branch2Node() {
		}

		public Branch2Node(Integer id, MiddleNode middleNode) {
			this.id = id;
			this.middleNode = middleNode;
		}
	}

	@Entity
	@Table(name="middle_node")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = MiddleNode.class, association = "terminalNode")
	})
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class MiddleNode {
		@Id
		private Integer id;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private TerminalNode terminalNode;

		public MiddleNode() {
		}

		public MiddleNode(Integer id, TerminalNode terminalNode) {
			this.id = id;
			this.terminalNode = terminalNode;
		}
	}

	@Entity
	@Table(name="terminal_node")
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class TerminalNode {
		@Id
		private Integer id;
		@Column(nullable = false)
		private String name;

		public TerminalNode() {
		}

		public TerminalNode(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
