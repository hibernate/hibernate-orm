/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PersistenceException;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * This tests the design demonstrated in the <a href=
 * 'https://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/Hibernate_User_Guide.html#identifiers-composite-nonaggregated'>user
 * guide</a>, example "&#64;{@link IdClass} with partial identifier generation using &#64;{@link GeneratedValue}". The
 * getters and setters have been omitted for clarity of the code. A separate test has been made for
 * {@link GenerationType#SEQUENCE}, {@link GenerationType#TABLE}, and
 * {@link GenerationType#AUTO} since there are known complications with some {@link Dialect}s (e.g.
 * {@link SQLServerDialect}) and the {@link GenerationType#IDENTITY}
 *
 * @author Jason Pyeron <support@pdinc.us>
 * @see <a href='https://hibernate.atlassian.net/browse/HHH-10956'>HHH-10956</a> Persisting partially-generated
 * composite Ids fails
 * @see <a href='https://hibernate.atlassian.net/browse/HHH-9662'>HHH-9662</a> a related and blocking bug for
 * {@link GenerationType#IDENTITY}
 * @see <a href='https://hibernate.atlassian.net/browse/HHH-4848'>HHH-4848</a> introduced the regression
 */
@JiraKey(value = "HHH-10956")
@DomainModel(
		annotatedClasses = {
				CompositeIdFkGeneratedValueTest.Head.class,
				CompositeIdFkGeneratedValueTest.Node.class,
				CompositeIdFkGeneratedValueTest.HeadS.class,
				CompositeIdFkGeneratedValueTest.NodeS.class,
				CompositeIdFkGeneratedValueTest.HeadA.class,
				CompositeIdFkGeneratedValueTest.NodeA.class,
				CompositeIdFkGeneratedValueTest.HeadT.class,
				CompositeIdFkGeneratedValueTest.NodeT.class,
				CompositeIdFkGeneratedValueTest.ComplexNodeS.class,
				CompositeIdFkGeneratedValueTest.ComplexNodeT.class,
				CompositeIdFkGeneratedValueTest.ComplexNodeA.class,
		}
)
@SessionFactory
public class CompositeIdFkGeneratedValueTest {

	@Test
	public void testCompositePkWithoutIdentifierGenerator(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Head head = new Head();
			head.name = "Head by Sequence";
			session.persist( head );
			System.out.println( "VALUE =>" + head.name + "=" + head.hid );

			Node node = new Node();
			node.nid = 1L;
			node.name = "Node by Sequence";
			node.hid = head;
			session.persist( node );
			System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid );
		} );

		scope.inTransaction( session -> {
			Head head = new Head();
			head.name = "Head by Sequence";
			session.persist( head );
			System.out.println( "VALUE =>" + head.name + "=" + head.hid );

			try {
				Node node = new Node();
				node.name = "Node by Sequence";
				node.hid = head;
				session.persist( node );
				System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid );

				session.flush();
				fail("A PersistenceException is expected, the Composite Id has a null value");
			}
			catch (PersistenceException e) {
				//expected Node.nid is null and hasn't an associated Identifier Generator
			}
		} );
	}

	@Test
	public void testCompositePkWithIdentityAndFKBySequence(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HeadS head = new HeadS();
			head.name = "Head by Sequence";
			session.persist( head );
			System.out.println( "VALUE =>" + head.name + "=" + head.hid );

			NodeS node = new NodeS();
			node.name = "Node by Sequence";
			node.hid = head;
			session.persist( node );
			System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid );
		} );
	}

	@Test
	public void testCompositePkWithSequenceGeneratorAndNullValue(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				NodeS node = new NodeS();
				node.name = "Node by Sequence";
				session.persist( node );
				System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid );

				session.flush();
				fail( "A PersistenceException is expected, the Composite Id has a null value" );
			}
			catch (PersistenceException e) {
				//expected, the NodeS.hid is null
			}
		} );
	}

	@Test
	public void testCompositePkWithIdentityAndFKByTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HeadT head = new HeadT();
			head.name = "Head by Table";
			session.persist( head );
			System.out.println( "VALUE =>" + head.name + "=" + head.hid );

			NodeT node = new NodeT();
			node.hid = head;
			node.name = "Node by Table";
			session.persist( node );
			System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid );
		} );
	}

	@Test
	public void testCompositePkWithIdentityAndFKByAuto(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HeadA head = new HeadA();
			head.name = "Head by Auto";
			session.persist( head );
			System.out.println( "VALUE =>" + head.name + "=" + head.hid );

			NodeA node = new NodeA();
			node.hid = head;
			node.name = "Node by Auto";
			session.persist( node );
			System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid );
		} );
	}

	@Test
	public void testCompositePkWithSequenceIdentifierGeneratorAndFKBySequence2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HeadS head = new HeadS();
			head.name = "Head by Sequence";
			session.persist( head );
			System.out.println( "VALUE =>" + head.name + "=" + head.hid );

			ComplexNodeS node = new ComplexNodeS();
			node.hid = head;
			node.name = "Node by Sequence";
			session.persist( node );
			System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid + " with parent="
					+ ( node.parent == null ? null : node.parent.nid + ":" + node.parent.hid.hid ) );

			ComplexNodeS node2 = new ComplexNodeS();
			node2.hid = head;
			node2.name = "Node 2 by Sequence";
			node2.parent = node;
			session.persist( node2 );
			System.out.println( "VALUE =>" + node2.name + "=" + node2.nid + ":" + node2.hid.hid + " with parent="
					+ ( node2.parent == null ? null : node2.parent.nid + ":" + node2.parent.hid.hid ) );
		} );
	}

	@Test
	public void testCompositePkWithIdentityAndFKByTable2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HeadT head = new HeadT();
			head.name = "Head by Table";
			session.persist( head );
			System.out.println( "VALUE =>" + head.name + "=" + head.hid );

			ComplexNodeT node = new ComplexNodeT();
			node.hid = head;
			node.name = "Node by Table";
			session.persist( node );
			System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid + " with parent="
					+ ( node.parent == null ? null : node.parent.nid + ":" + node.parent.hid.hid ) );

			ComplexNodeT node2 = new ComplexNodeT();
			node2.hid = head;
			node2.name = "Node 2 by Table";
			node2.parent = node;
			session.persist( node2 );
			System.out.println( "VALUE =>" + node2.name + "=" + node2.nid + ":" + node2.hid.hid + " with parent="
					+ ( node2.parent == null ? null : node2.parent.nid + ":" + node2.parent.hid.hid ) );
		} );
	}

	@Test
	public void testCompositePkWithIdentityAndFKByAuto2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HeadA head = new HeadA();
			head.name = "Head by Auto";
			session.persist( head );
			System.out.println( "VALUE =>" + head.name + "=" + head.hid );

			ComplexNodeA node = new ComplexNodeA();
			node.hid = head;
			node.name = "Node by Auto";
			session.persist( node );
			System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid + " with parent="
					+ ( node.parent == null ? null : node.parent.nid + ":" + node.parent.hid.hid ) );

			ComplexNodeA node2 = new ComplexNodeA();
			node2.hid = head;
			node2.name = "Node 2 by Auto";
			node2.parent = node;
			session.persist( node2 );
			System.out.println( "VALUE =>" + node2.name + "=" + node2.nid + ":" + node2.hid.hid + " with parent="
					+ ( node2.parent == null ? null : node2.parent.nid + ":" + node2.parent.hid.hid ) );
		} );
	}

	@Entity(name = "Head")
	public static class Head {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long hid;

		private String name;
	}

	@Entity(name = "Node")
	@IdClass(Node.PK.class)
	public static class Node {

		@Id
		private Long nid;

		@Id
		@ManyToOne
		private Head hid;

		private String name;

		public static class PK implements Serializable {

			private Long nid;

			private Long hid;

			public PK(Long nid, Long hid) {
				this.nid = nid;
				this.hid = hid;
			}

			private PK() {
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				PK pk = (PK) o;
				return Objects.equals( nid, pk.nid ) && Objects.equals( hid, pk.hid );
			}

			@Override
			public int hashCode() {
				return Objects.hash( nid, hid );
			}
		}

	}

	@Entity(name = "HeadS")
	public static class HeadS {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long hid;

		private String name;
	}

	@Entity(name = "NodeS")
	@IdClass(NodeS.PK.class)
	public static class NodeS {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long nid;

		@Id
		@ManyToOne
		private HeadS hid;

		private String name;

		public static class PK implements Serializable {

			private Long nid;

			private Long hid;

			public PK(Long nid, Long hid) {
				this.nid = nid;
				this.hid = hid;
			}

			private PK() {
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				PK pk = (PK) o;
				return Objects.equals( nid, pk.nid ) && Objects.equals( hid, pk.hid );
			}

			@Override
			public int hashCode() {
				return Objects.hash( nid, hid );
			}
		}

	}

	@Entity(name = "HeadA")
	public static class HeadA {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long hid;

		private String name;
	}

	@Entity(name = "NodeA")
	@IdClass(NodeA.PK.class)
	public static class NodeA {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long nid;

		@Id
		@ManyToOne
		private HeadA hid;

		private String name;

		public static class PK implements Serializable {

			private Long nid;

			private Long hid;

			public PK(Long nid, Long hid) {
				this.nid = nid;
				this.hid = hid;
			}

			private PK() {
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				PK pk = (PK) o;
				return Objects.equals( nid, pk.nid ) && Objects.equals( hid, pk.hid );
			}

			@Override
			public int hashCode() {
				return Objects.hash( nid, hid );
			}
		}

	}

	@Entity(name = "HeadT")
	public static class HeadT {

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private Long hid;

		private String name;
	}

	@Entity(name = "NodeT")
	@IdClass(NodeT.PK.class)
	public static class NodeT {

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private Long nid;

		@Id
		@ManyToOne
		private HeadT hid;

		private String name;

		public static class PK implements Serializable {

			private Long nid;

			private Long hid;

			public PK(Long nid, Long hid) {
				this.nid = nid;
				this.hid = hid;
			}

			private PK() {
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				PK pk = (PK) o;
				return Objects.equals( nid, pk.nid ) && Objects.equals( hid, pk.hid );
			}

			@Override
			public int hashCode() {
				return Objects.hash( nid, hid );
			}
		}

	}

	@Entity(name = "ComplexNodeS")
	@IdClass(ComplexNodeS.PK.class)
	public static class ComplexNodeS {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long nid;

		@Id
		@ManyToOne
		private HeadS hid;

		@ManyToOne
		private ComplexNodeS parent;

		private String name;

		public static class PK implements Serializable {

			private Long nid;

			private Long hid;

			public PK(Long nid, Long hid) {
				this.nid = nid;
				this.hid = hid;
			}

			private PK() {
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				PK pk = (PK) o;
				return Objects.equals( nid, pk.nid ) && Objects.equals( hid, pk.hid );
			}

			@Override
			public int hashCode() {
				return Objects.hash( nid, hid );
			}
		}

	}

	@Entity(name = "ComplexNodeT")
	@IdClass(ComplexNodeT.PK.class)
	public static class ComplexNodeT {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long nid;

		@Id
		@ManyToOne
		private HeadT hid;

		@ManyToOne
		private ComplexNodeT parent;

		private String name;

		public static class PK implements Serializable {

			private Long nid;

			private Long hid;

			public PK(Long nid, Long hid) {
				this.nid = nid;
				this.hid = hid;
			}

			private PK() {
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				PK pk = (PK) o;
				return Objects.equals( nid, pk.nid ) && Objects.equals( hid, pk.hid );
			}

			@Override
			public int hashCode() {
				return Objects.hash( nid, hid );
			}
		}

	}

	@Entity(name = "ComplexNodeA")
	@IdClass(ComplexNodeA.PK.class)
	public static class ComplexNodeA {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long nid;

		@Id
		@ManyToOne
		private HeadA hid;

		@ManyToOne
		private ComplexNodeA parent;

		private String name;

		public static class PK implements Serializable {

			private Long nid;

			private Long hid;

			public PK(Long nid, Long hid) {
				this.nid = nid;
				this.hid = hid;
			}

			private PK() {
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				PK pk = (PK) o;
				return Objects.equals( nid, pk.nid ) && Objects.equals( hid, pk.hid );
			}

			@Override
			public int hashCode() {
				return Objects.hash( nid, hid );
			}
		}

	}

}
