/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cid;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This tests the design demonstrated in the <a href=
 * 'https://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/Hibernate_User_Guide.html#identifiers-composite-nonaggregated'>user
 * guide</a>, example "&#64;{@link IdClass} with partial identifier generation using &#64;{@link GeneratedValue}". The
 * getters and setters have been omitted for clarity of the code. A separate test has been made for
 * {@link GenerationType#SEQUENCE}, {@link GenerationType#IDENTITY}, {@link GenerationType#TABLE}, and
 * {@link GenerationType#AUTO} since there are known complications with some {@link Dialect}s (e.g.
 * {@link SQLServer2012Dialect}) and the {@link GenerationType#IDENTITY}
 *
 * @author Jason Pyeron <support@pdinc.us>
 * @see <a href='https://hibernate.atlassian.net/browse/HHH-10956'>HHH-10956</a> Persisting partially-generated
 * composite Ids fails
 * @see <a href='https://hibernate.atlassian.net/browse/HHH-9662'>HHH-9662</a> a related and blocking bug for
 * {@link GenerationType#IDENTITY}
 * @see <a href='https://hibernate.atlassian.net/browse/HHH-4848'>HHH-4848</a> introduced the regression
 */
@TestForIssue(jiraKey = "HHH-10956")
public class CompositeIdFkGeneratedValueIdentityTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testCompositePkWithIdentityAndFKBySequence() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			HeadS head = new HeadS();
			head.name = "Head by Sequence";
			session.persist( head );
			System.out.println( "VALUE =>" + head.name + "=" + head.hid );

			NodeS node = new NodeS();
			node.hid = head;
			node.name = "Node by Sequence";
			session.persist( node );
			System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid );
		} );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-9662", message = "Could not set field value [POST_INSERT_INDICATOR]")
	public void testCompositePkWithIdentityAndFKByIdentity() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			HeadI head = new HeadI();
			head.name = "Head by Identity";
			session.persist( head );
			System.out.println( "VALUE =>" + head.name + "=" + head.hid );

			NodeI node = new NodeI();
			node.hid = head;
			node.name = "Node by Identity";
			try {
				session.persist( node );
			}
			catch (Error | RuntimeException e) {
				// expected failure...
				e.printStackTrace( System.out );
				throw e;
			}
			System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid );
		} );
	}

	@Test
	public void testCompositePkWithIdentityAndFKByTable() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
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
	public void testCompositePkWithIdentityAndFKByAuto() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
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
	public void testCompositePkWithIdentityAndFKBySequence2() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
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
	@Ignore("HHH-13971 - unit test DB is using call next value for hibernate_sequence when it should be Identity insert")
	@FailureExpected(jiraKey = "HHH-9662", message = "Could not set field value [POST_INSERT_INDICATOR]")
	public void testCompositePkWithIdentityAndFKByIdentity2() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			HeadI head = new HeadI();
			head.name = "Head by Identity";
			session.persist( head );
			System.out.println( "VALUE =>" + head.name + "=" + head.hid );

			ComplexNodeI node = new ComplexNodeI();
			node.hid = head;
			node.name = "Node by Identity";
			session.persist( node );
			System.out.println( "VALUE =>" + node.name + "=" + node.nid + ":" + node.hid.hid + " with parent="
					+ ( node.parent == null ? null : node.parent.nid + ":" + node.parent.hid.hid ) );

			ComplexNodeI node2 = new ComplexNodeI();
			node2.hid = head;
			node2.name = "Node 2 by Identity";
			node2.parent = node;
			session.persist( node2 );
			System.out.println( "VALUE =>" + node2.name + "=" + node2.nid + ":" + node2.hid.hid + " with parent="
					+ ( node2.parent == null ? null : node2.parent.nid + ":" + node2.parent.hid.hid ) );
		} );
	}

	@Test
	public void testCompositePkWithIdentityAndFKByTable2() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
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
	public void testCompositePkWithIdentityAndFKByAuto2() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				HeadS.class,
				NodeS.class,
				HeadA.class,
				NodeA.class,
				HeadI.class,
				NodeI.class,
				HeadT.class,
				NodeT.class,
				ComplexNodeS.class,
				ComplexNodeI.class,
				ComplexNodeT.class,
				ComplexNodeA.class,
		};
	}

	@Entity
	public static class HeadS {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long hid;

		private String name;
	}

	@Entity
	@IdClass(CompositeIdFkGeneratedValueIdentityTest.NodeS.PK.class)
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

	@Entity
	public static class HeadI {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long hid;

		private String name;
	}

	@Entity
	@IdClass(CompositeIdFkGeneratedValueIdentityTest.NodeI.PK.class)
	public static class NodeI {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long nid;

		@Id
		@ManyToOne
		private HeadI hid;

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

	@Entity
	public static class HeadA {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long hid;

		private String name;
	}

	@Entity
	@IdClass(CompositeIdFkGeneratedValueIdentityTest.NodeA.PK.class)
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

	@Entity
	public static class HeadT {

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private Long hid;

		private String name;
	}

	@Entity
	@IdClass(CompositeIdFkGeneratedValueIdentityTest.NodeT.PK.class)
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

	@Entity
	@IdClass(CompositeIdFkGeneratedValueIdentityTest.ComplexNodeS.PK.class)
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

	@Entity
	@IdClass(CompositeIdFkGeneratedValueIdentityTest.ComplexNodeI.PK.class)
	public static class ComplexNodeI {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long nid;

		@Id
		@ManyToOne
		private HeadI hid;

		@ManyToOne
		private ComplexNodeI parent;

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

	@Entity
	@IdClass(CompositeIdFkGeneratedValueIdentityTest.ComplexNodeT.PK.class)
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

	@Entity
	@IdClass(CompositeIdFkGeneratedValueIdentityTest.ComplexNodeA.PK.class)
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
