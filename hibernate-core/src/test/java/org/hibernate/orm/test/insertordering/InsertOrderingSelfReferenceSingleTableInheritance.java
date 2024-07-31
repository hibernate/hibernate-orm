/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.insertordering;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Harikant Verma
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-13068")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
@Jpa(
		annotatedClasses = {
				InsertOrderingSelfReferenceSingleTableInheritance.ContentNode.class,
				InsertOrderingSelfReferenceSingleTableInheritance.EntityTreeNode.class,
				InsertOrderingSelfReferenceSingleTableInheritance.NodeLink.class,
				InsertOrderingSelfReferenceSingleTableInheritance.NodeLongValue.class,
				InsertOrderingSelfReferenceSingleTableInheritance.NodeStringValue.class,
				InsertOrderingSelfReferenceSingleTableInheritance.ReferNode.class,
				InsertOrderingSelfReferenceSingleTableInheritance.TreeNodeValue.class,
		}
)
@ServiceRegistry(
		settings = {
				@Setting(value = Environment.ORDER_INSERTS, name = "true"),
				@Setting(value = Environment.STATEMENT_BATCH_SIZE, name = "10")
		}
)
public class InsertOrderingSelfReferenceSingleTableInheritance {

	@Test
	public void test1(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			NodeLongValue longVal = new NodeLongValue();
			longVal.setLongValue( 123L );
			NodeStringValue stringVal = new NodeStringValue();
			stringVal.setStringValue( "Node 123" );

			EntityTreeNode etn = new EntityTreeNode( null, null );
			ContentNode cn1 = new ContentNode( stringVal, null, null );
			ContentNode cn2 = new ContentNode( longVal, cn1, null );

			NodeLink nl = new NodeLink( cn2 );

			ReferNode rn1 = new ReferNode( etn, null, nl );

			entityManager.persist( rn1 );
		} );
	}

	@Test
	public void test2(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			ContentNode xa = new ContentNode(new NodeLongValue(123L), null, null );
			ContentNode xb = new ContentNode(new NodeLongValue(123L), null, null );
			ContentNode xc = new ContentNode(new NodeLongValue(123L), xb, null );

			NodeLink nl = new NodeLink( xc );

			ReferNode ya = new ReferNode( xa, null, nl );

			entityManager.persist( ya );
		} );
	}

	@Test
	public void test3(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			NodeLongValue longVal = new NodeLongValue();
			longVal.setLongValue( 123L );
			NodeStringValue stringVal = new NodeStringValue();
			stringVal.setStringValue( "Node 123" );

			EntityTreeNode etn = new EntityTreeNode( null, null );
			ContentNode cn1 = new ContentNode( stringVal, null, null );
			ContentNode cn2 = new ContentNode( longVal, cn1, null );

			ReferNode rn1 = new ReferNode( etn, cn2, null );

			entityManager.persist( rn1 );
		} );
	}

	@Test
	public void test4(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			NodeLongValue longVal = new NodeLongValue();
			longVal.setLongValue( 123L );
			NodeStringValue stringVal = new NodeStringValue();
			stringVal.setStringValue( "Node 123" );

			ContentNode cn0 = new ContentNode( null, null, null );
			ContentNode cn1 = new ContentNode( stringVal, null, null );
			ContentNode cn2 = new ContentNode( longVal, cn0, null );

			ContentNode cn3 = new ContentNode( null, cn1, cn2 );

			entityManager.persist( cn3 );
		} );
	}

	@Test
	public void test5(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			NodeLongValue longVal = new NodeLongValue();
			longVal.setLongValue( 123L );
			NodeStringValue stringVal = new NodeStringValue();
			stringVal.setStringValue( "Node 123" );

			EntityTreeNode etn = new EntityTreeNode( null, null );
			ContentNode cn1 = new ContentNode( stringVal, null, null );
			ContentNode cn2 = new ContentNode( longVal, cn1, null );

			ContentNode cn3 = new ContentNode( null, etn, cn2 );

			entityManager.persist( cn3 );
		} );
	}

	@Test
	public void test6(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			NodeLongValue longVal = new NodeLongValue();
			longVal.setLongValue( 123L );
			NodeStringValue stringVal = new NodeStringValue();
			stringVal.setStringValue( "Node 123" );

			EntityTreeNode etn = new EntityTreeNode( null, null );
			ContentNode cn1 = new ContentNode( stringVal, null, null );
			ReferNode rn1 = new ReferNode( null, cn1, null );

			ContentNode cn3 = new ContentNode( longVal, etn, rn1 );

			entityManager.persist( cn3 );
		} );
	}

	@Test
	public void test7(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			NodeStringValue stringVal = new NodeStringValue();
			stringVal.setStringValue( "Node 123" );

			EntityTreeNode etn = new EntityTreeNode( null, null );
			ContentNode cn1 = new ContentNode( null, etn, null );
			ReferNode rn1 = new ReferNode( null, cn1, null );

			entityManager.persist( rn1 );
		} );
	}

	@Test
	public void test8(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			NodeStringValue stringVal = new NodeStringValue();
			stringVal.setStringValue( "Node 123" );

			EntityTreeNode etn = new EntityTreeNode( null, null );
			ReferNode rn1 = new ReferNode( null, etn, null );
			ContentNode cn3 = new ContentNode( null, rn1, null );

			entityManager.persist( cn3 );
		} );
	}

	@Test
	public void test9(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			NodeStringValue stringVal = new NodeStringValue();
			stringVal.setStringValue( "Node 123" );

			EntityTreeNode etn = new EntityTreeNode( null, null );
			ContentNode cn1 = new ContentNode( null, etn, null );
			ReferNode rn1 = new ReferNode( null, cn1, null );

			ContentNode cn3 = new ContentNode( null, rn1, null );

			entityManager.persist( cn3 );
		} );
	}

	@Entity(name = "EntityTreeNode")
	@DynamicUpdate
	@DynamicInsert
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class EntityTreeNode {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		private EntityTreeNode leftNode;

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		private EntityTreeNode rightNode;

		public EntityTreeNode(EntityTreeNode leftNode, EntityTreeNode rightNode) {
			super();
			this.leftNode = leftNode;
			this.rightNode = rightNode;
		}

		public EntityTreeNode() {
			super();
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public EntityTreeNode getLeftNode() {
			return leftNode;
		}

		public void setLeftNode(EntityTreeNode leftNode) {
			this.leftNode = leftNode;
		}

		public EntityTreeNode getRightNode() {
			return rightNode;
		}

		public void setRightNode(EntityTreeNode rightNode) {
			this.rightNode = rightNode;
		}
	}

	@Entity(name = "ContentNode")
	@DynamicUpdate
	@DynamicInsert
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class ContentNode extends EntityTreeNode {

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
		private TreeNodeValue nodeValue;

		public TreeNodeValue getNodeValue() {
			return nodeValue;
		}

		public void setNodeValue(TreeNodeValue nodeValue) {
			this.nodeValue = nodeValue;
		}

		public ContentNode(TreeNodeValue nodeValue, EntityTreeNode leftNode, EntityTreeNode rightNode) {
			super( leftNode, rightNode );
			this.nodeValue = nodeValue;
		}

		public ContentNode() {
			super();
		}

	}

	@Entity(name = "NodeLink")
	@DynamicUpdate
	@DynamicInsert
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class NodeLink {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
		private ContentNode toNode;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public ContentNode getToNode() {
			return this.toNode;
		}

		public void setToNode(ContentNode toNode) {
			this.toNode = toNode;
		}

		public NodeLink(ContentNode toNode) {
			super();
			setToNode( toNode );
		}

		public NodeLink() {
			super();
		}
	}

	@Entity(name = "NodeLongValue")
	@DynamicUpdate
	@DynamicInsert
	public static class NodeLongValue extends TreeNodeValue {

		Long longValue;

		public Long getLongValue() {
			return longValue;
		}

		public void setLongValue(Long longValue) {
			this.longValue = longValue;
		}

		public NodeLongValue(String dataType, Long longValue) {
			super( dataType );
			this.longValue = longValue;
		}

		public NodeLongValue(Long longValue) {
			super();
			this.longValue = longValue;
		}

		public NodeLongValue() {
			super();
		}
	}

	@Entity(name = "NodeStringValue")
	@DynamicUpdate
	@DynamicInsert
	public static class NodeStringValue extends TreeNodeValue {

		String stringValue;

		public String getStringValue() {
			return stringValue;
		}

		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}

		public NodeStringValue(String dataType, String stringValue) {
			super( dataType );
			this.stringValue = stringValue;
		}

		public NodeStringValue() {
			super();
		}
	}

	@Entity(name = "ReferNode")
	@DynamicUpdate
	@DynamicInsert
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class ReferNode extends EntityTreeNode {

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
		private NodeLink nodeLink;

		public NodeLink getNodeLink() {
			return nodeLink;
		}

		public void setNodeLink(NodeLink nodeLink) {
			this.nodeLink = nodeLink;
		}

		public ReferNode(EntityTreeNode leftNode, EntityTreeNode rightNode, NodeLink nodeLink) {
			super( leftNode, rightNode );
			this.nodeLink = nodeLink;
		}

		public ReferNode() {
			super();
		}
	}

	@Entity(name = "TreeNodeValue")
	@DynamicUpdate
	@DynamicInsert
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class TreeNodeValue {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String dataType;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getDataType() {
			return dataType;
		}

		public void setDataType(String dataType) {
			this.dataType = dataType;
		}

		public TreeNodeValue(String dataType) {
			super();
			this.dataType = dataType;
		}

		public TreeNodeValue() {
			super();
		}
	}

}
