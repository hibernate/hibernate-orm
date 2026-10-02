/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@DomainModel(
		annotatedClasses = {
				GetLoadTest.Node.class,
				GetLoadTest.Employer.class,
				GetLoadTest.Employee.class
		}
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0")
		}
)
public class GetLoadTest {

	@Test
	public void testGetLoad(SessionFactoryScope scope) {
		clearCounts( scope );

		Employer emp = new Employer();
		Node node = new Node( "foo" );
		Node parent = new Node( "bar" );
		scope.inTransaction(
				session -> {
					session.persist( emp );
					parent.addChild( node );
					session.persist( parent );
				}
		);

		scope.inTransaction(
				session -> {
					Employer e = session.get( Employer.class, emp.getId() );
					assertTrue( Hibernate.isInitialized( e ) );
					assertFalse( Hibernate.isInitialized( e.getEmployees() ) );
					Node n = session.get( Node.class, node.getName() );
					assertTrue( Hibernate.isInitialized( n ) );
					assertFalse( Hibernate.isInitialized( n.getChildren() ) );
					assertFalse( Hibernate.isInitialized( n.getParent() ) );
					assertNull( session.find( Node.class, "xyz" ) );
				}
		);

		scope.inTransaction(
				session -> {
					Employer e = session.getReference( Employer.class, emp.getId() );
					e.getId();
					assertFalse( Hibernate.isInitialized( e ) );
					Node n = session.getReference( Node.class, node.getName() );
					assertThat( n.getName(), is( "foo" ) );
					assertFalse( Hibernate.isInitialized( n ) );
				}
		);

		scope.inTransaction(
				session -> {
					Employer e = (Employer) session.get( "org.hibernate.orm.test.ops.GetLoadTest$Employer", emp.getId() );
					assertTrue( Hibernate.isInitialized( e ) );
					Node n = (Node) session.get( "org.hibernate.orm.test.ops.GetLoadTest$Node", node.getName() );
					assertTrue( Hibernate.isInitialized( n ) );
				}
		);

		scope.inTransaction(
				session -> {
					Employer e = (Employer) session.getReference( "org.hibernate.orm.test.ops.GetLoadTest$Employer", emp.getId() );
					e.getId();
					assertFalse( Hibernate.isInitialized( e ) );
					Node n = (Node) session.getReference( "org.hibernate.orm.test.ops.GetLoadTest$Node", node.getName() );
					assertThat( n.getName(), is( "foo" ) );
					assertFalse( Hibernate.isInitialized( n ) );
				}
		);

		assertFetchCount( 0, scope );
	}

	@Test
	public void testGetAfterDelete(SessionFactoryScope scope) {
		clearCounts( scope );

		Employer emp = new Employer();

		scope.inTransaction(
				session ->
						session.persist( emp )
		);

		Employer e = scope.fromTransaction(
				session -> {
					session.remove( emp );
					return session.find( Employer.class, emp.getId() );
				}
		);

		assertNull( e, "get did not return null after delete" );
	}

	private void clearCounts(SessionFactoryScope scope) {
		scope.getSessionFactory().getStatistics().clear();
	}

	private void assertFetchCount(int count, SessionFactoryScope scope) {
		int fetches = (int) scope.getSessionFactory().getStatistics().getEntityFetchCount();
		assertThat( fetches, is( count ) );
	}

	@Entity(name = "Node")
	@Table(name = "Node")
	public static class Node {
		@Id
		private String name;
		private String description;
		@Column(nullable = false)
		private Date created;
		@ManyToOne(fetch = FetchType.LAZY)
		private Node parent;
		@OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH})
		private Set<Node> children = new HashSet<>();
		@OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REMOVE})
		@JoinColumn(name = "CASC_PARENT")
		private Set<Node> cascadingChildren = new HashSet<>();

		public Node() {
		}

		public Node(String name) {
			this.name = name;
			created = new Date( new java.util.Date().getTime() );
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Date getCreated() {
			return created;
		}

		public void setCreated(Date created) {
			this.created = created;
		}

		public Node getParent() {
			return parent;
		}

		public void setParent(Node parent) {
			this.parent = parent;
		}

		public Set<Node> getChildren() {
			return children;
		}

		public void setChildren(Set<Node> children) {
			this.children = children;
		}

		public Node addChild(Node child) {
			children.add( child );
			child.setParent( this );
			return this;
		}

		public Set<Node> getCascadingChildren() {
			return cascadingChildren;
		}

		public void setCascadingChildren(Set<Node> cascadingChildren) {
			this.cascadingChildren = cascadingChildren;
		}
	}

	@Entity(name = "Employer")
	@Table(name = "Employer")
	public static class Employer {
		@Id
		@GeneratedValue
		private Integer id;
		@Version
		@Column(name = "vers")
		private Integer vers;
		@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
		@JoinTable(name = "EMPLOYER_EMPLOYEE",
				joinColumns = @jakarta.persistence.JoinColumn(name = "EMPER_ID"),
				inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "EMPEE_ID"))
		private Collection<Employee> employees = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getVers() {
			return vers;
		}

		public void setVers(Integer vers) {
			this.vers = vers;
		}

		public Collection<Employee> getEmployees() {
			return employees;
		}

		public void setEmployees(Collection<Employee> employees) {
			this.employees = employees;
		}
	}

	@Entity(name = "Employee")
	@Table(name = "Employee")
	public static class Employee implements Serializable {
		@Id
		@GeneratedValue
		private Integer id;
		@ManyToMany(mappedBy = "employees", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
		private Collection<Employer> employers = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Collection<Employer> getEmployers() {
			return employers;
		}

		public void setEmployers(Collection<Employer> employers) {
			this.employers = employers;
		}
	}
}
