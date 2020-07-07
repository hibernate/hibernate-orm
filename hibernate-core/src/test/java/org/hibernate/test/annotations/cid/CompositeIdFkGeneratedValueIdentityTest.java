/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.annotations.cid;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@TestForIssue(jiraKey = "HHH-10956")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class CompositeIdFkGeneratedValueIdentityTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				HeadI.class,
				NodeI.class,
				ComplexNodeI.class,
		};
	}

	@Test
	@FailureExpected(jiraKey = "HHH-9662", message = "Could not set field value [POST_INSERT_INDICATOR]")
	public void testCompositePkWithIdentityAndFKByIdentity() {
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
	public void testCompositePkWithIdentityAndFKByIdentity2() {
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
										+ ( node.parent == null ?
					null :
					node.parent.nid + ":" + node.parent.hid.hid ) );

			ComplexNodeI node2 = new ComplexNodeI();
			node2.hid = head;
			node2.name = "Node 2 by Identity";
			node2.parent = node;
			session.persist( node2 );
			System.out.println( "VALUE =>" + node2.name + "=" + node2.nid + ":" + node2.hid.hid + " with parent="
										+ ( node2.parent == null ?
					null :
					node2.parent.nid + ":" + node2.parent.hid.hid ) );
		} );
	}

	@Entity(name = "HeadI")
	public static class HeadI {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long hid;

		private String name;
	}

	@Entity(name = "NodeI")
	@IdClass(NodeI.PK.class)
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

	@Entity(name = "ComplexNodeI")
	@IdClass(ComplexNodeI.PK.class)
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
}
