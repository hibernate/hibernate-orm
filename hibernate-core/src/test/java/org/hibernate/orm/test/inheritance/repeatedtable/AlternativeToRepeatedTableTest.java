/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.repeatedtable;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hibernate.cfg.AvailableSettings.FORMAT_SQL;
import static org.hibernate.cfg.AvailableSettings.SHOW_SQL;

@JiraKey(value = "HHH-15932")
public class AlternativeToRepeatedTableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				DataType.class,
				ObjectType.class,
				SimpleType.class,
				Prop.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure(configuration);
		configuration.setProperty(SHOW_SQL, true);
		configuration.setProperty(FORMAT_SQL, true);
	}

	@Test
	public void test_append_properties()  {
		Long id;
		Long sId;
		try (Session sess = openSession()) {
			Transaction tx = sess.beginTransaction();

			SimpleType simpleType = new SimpleType();
			simpleType.setName("simple");
			simpleType.setCount(69);
			sess.persist(simpleType);
			sId = simpleType.getId();

			ObjectType objectType = new ObjectType();
			objectType.setName("name");
			sess.persist(objectType);
			id = objectType.getId();

			tx.commit();
		}

		try (Session sess = openSession()) {
			Transaction tx = sess.beginTransaction();
			ObjectType objectType = sess.find(ObjectType.class, id);
			Prop property = new Prop();
			property.setName("Prop1");
			property.setObjectType(objectType);
			objectType.getProperties().add(property);
			tx.commit();
		}
		try (Session sess = openSession()) {
			Transaction tx = sess.beginTransaction();
			ObjectType objectType = sess.find(ObjectType.class, id);
			assertEquals(1, objectType.getProperties().size());
			tx.commit();
		}

		try (Session sess = openSession()) {
			DataType dataType1 = sess.find(DataType.class, sId);
			assertTrue( dataType1 instanceof SimpleType );
			DataType dataType2 = sess.find(DataType.class, id);
			assertTrue( dataType2 instanceof ObjectType );
		}
		try (Session sess = openSession()) {
			SimpleType simpleType = sess.find(SimpleType.class, sId);
			assertNotNull( simpleType );
			SimpleType wrongType = sess.find(SimpleType.class, id);
			assertNull( wrongType );
		}

		try (Session sess = openSession()) {
			assertEquals( "Prop1",
					sess.createQuery("select p.name from AlternativeToRepeatedTableTest$ObjectType ot join ot.properties p")
							.getSingleResult() );
		}

		try (Session sess = openSession()) {
			assertEquals( 2, sess.createQuery("from AlternativeToRepeatedTableTest$DataType").getResultList().size() );
			assertEquals( 1, sess.createQuery("from AlternativeToRepeatedTableTest$ObjectType").getResultList().size() );
			assertEquals( 1, sess.createQuery("from AlternativeToRepeatedTableTest$SimpleType").getResultList().size() );
		}
	}

	@Entity
	@Table(name = "DATA_TYPE")
	@Inheritance(strategy = SINGLE_TABLE)
	@DiscriminatorColumn(name = "supertype_id")
	public static abstract class DataType {

		private Long id;
		private String name;

		@Id
		@Column(name = "ID")
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Column(name = "name")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@Entity
	@DiscriminatorValue("8")
	@SecondaryTable(name = "OBJ_TYPE",
			pkJoinColumns = @PrimaryKeyJoinColumn(name = "TYPE_ID", referencedColumnName = "ID"))
	@SecondaryRow(optional = false)
	public static class ObjectType extends DataType {

		private String description;
		private List<Prop> properties;

		@Column(name = "descr", table = "OBJ_TYPE")
		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		@OneToMany(mappedBy = "objectType", cascade = ALL, orphanRemoval = true)
		public List<Prop> getProperties() {
			return properties;
		}

		public void setProperties(List<Prop> properties) {
			this.properties = properties;
		}
	}

	@Entity
	@Table(name = "PROP")
	public static class Prop {

		private Long id;
		private String name;
		private ObjectType objectType;

		@Id
		@Column(name = "ID")
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Column(name = "name")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

//		@JoinColumn(name = "OBJ_TYPE_ID")
//		@JoinColumn(name = "OBJ_TYPE_ID", referencedColumnName = "ID")
		@JoinColumn(name = "OBJ_TYPE_ID", referencedColumnName = "TYPE_ID")
		@ManyToOne
		public ObjectType getObjectType() {
			return objectType;
		}

		public void setObjectType(ObjectType objectType) {
			this.objectType = objectType;
		}
	}

	@Entity
	@DiscriminatorValue("2")
	public static class SimpleType extends DataType {
		Integer count;

		@Column(name = "counter")
		public Integer getCount() {
			return count;
		}

		public void setCount(Integer count) {
			this.count = count;
		}
	}

}
