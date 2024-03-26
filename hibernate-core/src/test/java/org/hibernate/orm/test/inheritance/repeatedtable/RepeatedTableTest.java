package org.hibernate.orm.test.inheritance.repeatedtable;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.InheritanceType.JOINED;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hibernate.cfg.AvailableSettings.FORMAT_SQL;
import static org.hibernate.cfg.AvailableSettings.SHOW_SQL;

@TestForIssue(jiraKey = "HHH-14526")
public class RepeatedTableTest extends BaseCoreFunctionalTestCase {

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
			assertEquals( 2, sess.createQuery("from RepeatedTableTest$DataType").getResultList().size() );
			assertEquals( 1, sess.createQuery("from RepeatedTableTest$ObjectType").getResultList().size() );
			assertEquals( 1, sess.createQuery("from RepeatedTableTest$SimpleType").getResultList().size() );
		}
	}

	@Entity
	@Table(name = "DATA_TYPE")
	@Inheritance(strategy = JOINED)
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
	@Table(name = "OBJ_TYPE")
	@PrimaryKeyJoinColumn(name = "TYPE_ID")
	public static class ObjectType extends DataType {

		private String description;
		private List<Prop> properties;

		@Column(name = "descr")
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

		@JoinColumn(name = "OBJ_TYPE_ID")
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
	@Table(name = "DATA_TYPE")
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
