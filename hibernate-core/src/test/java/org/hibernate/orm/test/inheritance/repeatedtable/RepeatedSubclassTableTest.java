/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.repeatedtable;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.InheritanceType.JOINED;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		RepeatedSubclassTableTest.DataType.class,
		RepeatedSubclassTableTest.ObjectType.class,
		RepeatedSubclassTableTest.SimpleType.class,
		RepeatedSubclassTableTest.Prop.class
})
@SessionFactory
public class RepeatedSubclassTableTest {
	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.dropData();
	}

	@Test
	public void test_append_properties(SessionFactoryScope factoryScope)  {
		factoryScope.inTransaction( (sess) -> {
			var simpleType = new SimpleType( 1, "simple", 69 );
			sess.persist(simpleType);

			var objectType = new ObjectType( 2, "name", "stuff" );
			sess.persist(objectType);
		} );

		factoryScope.inTransaction( (sess) -> {
			var objectType = sess.find(ObjectType.class, 2);
			var property = new Prop( 1, "Prop1", objectType );
			objectType.getProperties().add(property);
		} );

		factoryScope.inTransaction( (sess) -> {
			var objectType = sess.find(ObjectType.class, 2);
			Assertions.assertEquals( 1, objectType.getProperties().size() );
		} );

		factoryScope.inTransaction( (sess) -> {
			var dataType1 = sess.find(DataType.class, 1);
			Assertions.assertInstanceOf( SimpleType.class, dataType1 );
			var dataType2 = sess.find(DataType.class, 2);
			Assertions.assertInstanceOf( ObjectType.class, dataType2 );
		} );

		factoryScope.inTransaction( (sess) -> {
			var simpleType = sess.find(SimpleType.class, 1);
			Assertions.assertNotNull( simpleType );
			SimpleType wrongType = sess.find(SimpleType.class, 2);
			Assertions.assertNull( wrongType );
		} );

		factoryScope.inTransaction( (sess) -> {
			//noinspection deprecation
			Assertions.assertEquals( 2,
					sess.createQuery("from DataType").getResultList().size() );
			//noinspection deprecation
			Assertions.assertEquals( 1,
					sess.createQuery("from ObjectType").getResultList().size() );
			//noinspection deprecation
			Assertions.assertEquals( 1,
					sess.createQuery("from SimpleType").getResultList().size() );
		} );
	}

	@Entity(name = "DataType")
	@Table(name = "DATA_TYPE")
	@Inheritance(strategy = JOINED)
	@DiscriminatorColumn(name = "supertype_id")
	public static abstract class DataType {
		private Integer id;
		private String name;

		protected DataType() {
		}

		public DataType(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		@Column(name = "ID")
		public Integer getId() {
			return id;
		}

		protected void setId(Integer id) {
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

	@Entity(name = "ObjectType")
	@DiscriminatorValue("8")
	@Table(name = "OBJ_TYPE")
	@PrimaryKeyJoinColumn(name = "TYPE_ID")
	public static class ObjectType extends DataType {
		private String description;
		private List<Prop> properties;

		protected ObjectType() {
		}

		public ObjectType(Integer id, String name, String description) {
			super( id, name );
			this.description = description;
		}

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

	@Entity(name = "Prop")
	@Table(name = "PROP")
	public static class Prop {
		private Integer id;
		private String name;
		private ObjectType objectType;

		protected Prop() {
		}

		public Prop(int id, String name, ObjectType objectType) {
			this.id = id;
			this.name = name;
			this.objectType = objectType;
		}

		@Id
		@Column(name = "ID")
		public Integer getId() {
			return id;
		}

		protected void setId(Integer id) {
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

	@Entity(name = "SimpleType")
	@DiscriminatorValue("2")
	@Table(name = "OBJ_TYPE")
	@PrimaryKeyJoinColumn(name = "TYPE_ID")
	public static class SimpleType extends DataType {
		Integer count;

		protected SimpleType() {
		}

		public SimpleType(Integer id, String name, Integer count) {
			super( id, name );
			this.count = count;
		}

		@Column(name = "counter")
		public Integer getCount() {
			return count;
		}

		public void setCount(Integer count) {
			this.count = count;
		}
	}

}
