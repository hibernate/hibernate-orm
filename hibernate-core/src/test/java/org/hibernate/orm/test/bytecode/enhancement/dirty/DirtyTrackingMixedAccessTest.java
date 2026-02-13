/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Test for HHH-19917: Bytecode-enhanced dirty tracking fails for mixed access
 * properties.
 */
@JiraKey("HHH-19917")
@DomainModel(annotatedClasses = {
		DirtyTrackingMixedAccessTest.DataTypes.class,
		DirtyTrackingMixedAccessTest.PropertyDefaultEntity.class
})
@SessionFactory
@BytecodeEnhanced
public class DirtyTrackingMixedAccessTest {

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}


	@Test
	public void testFieldDefaultWithPropertyAccessOverride(SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			DataTypes d1 = new DataTypes();
			d1.id = 1;
			d1.setIntData2(100);
			s.persist(d1);
		});

		scope.inTransaction(s -> {
			DataTypes d1 = s.get(DataTypes.class, 1);
			EnhancerTestUtils.clearDirtyTracking(d1);
			d1.setIntData2(200);
			EnhancerTestUtils.checkDirtyTracking(d1, "intData2");
			d1.setIntData2(100);
			EnhancerTestUtils.checkDirtyTracking(d1, "intData2");
		});
	}

	@Test
	public void testPropertyDefaultWithFieldAccessOverride(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			PropertyDefaultEntity e = new PropertyDefaultEntity();
			e.setId( 2 );
			e.setName( "original" );
			s.persist( e );
		} );
		scope.inTransaction( s -> {
			PropertyDefaultEntity e = s.get( PropertyDefaultEntity.class, 2 );
			EnhancerTestUtils.clearDirtyTracking( e );
			e.setName( "updated" );
			EnhancerTestUtils.checkDirtyTracking( e, "name" );
		} );
	}

	@Entity
	@Table(name = "DataTypes")
	@Access(AccessType.FIELD)
	public static class DataTypes {
		@Id
		protected int id;

		@Transient
		private int intData2;

		@Access(AccessType.PROPERTY)
		@Column(name = "INTDATA2")
		public int getIntData2() {
			return intData2;
		}

		public void setIntData2(int intData2) {
			this.intData2 = intData2;
		}
	}

	/**
	 * Vice-versa case: class default is PROPERTY, field overrides with @Access(FIELD),
	 * and getter is @Transient to avoid duplicate mapping.
	 */
	@Entity
	@Table(name = "PropertyDefaultEntity")
	@Access(AccessType.PROPERTY)
	public static class PropertyDefaultEntity {
		@Id
		@Access(AccessType.FIELD)
		@Column(name = "ENTITY_ID")
		private int id;
		private String name;
		@Transient
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		@Column(name = "NAME_COL")
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
}
