/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.singletable;

import java.util.List;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = {
				SingleTableOneToOneTest.Container1.class, SingleTableOneToOneTest.Container2.class, SingleTableOneToOneTest.BaseClass.class, SingleTableOneToOneTest.SubClass2.class, SingleTableOneToOneTest.SubClass1.class
		}
)
@SessionFactory
@JiraKeyGroup( value = {
		@JiraKey( value = "HHH-16916"),
		@JiraKey( value = "HHH-17228")
} )
public class SingleTableOneToOneTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true,
			reason = "SybaseDialect ignores unique constraints on nullable columns")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "InformixDialect ignores unique constraints on nullable columns")
	public void testMultipleRelationshipsOnSingleTableInheritanceWronglyMappedAsOneToOne(SessionFactoryScope scope) {
		assertThrows(
				ConstraintViolationException.class,
				() -> scope.inTransaction(
						session -> {
							SubClass1 subClass11 = new SubClass1();
							subClass11.setId(11L);

							SubClass2 subClass21 = new SubClass2();
							subClass21.setId(21L);

							Container1 container = new Container1();
							container.setId( 1L );
							container.setSubClass11( subClass11 );
							container.setSubClass21( subClass21 );
							subClass11.set1To1Container( container );
							subClass21.set1To1Container( container );

							session.persist( container );
						}
				)
		);
	}

	@Test
	public void testMultipleRelationshipsOnSingleTableInheritanceCorrectlyMappedAsManyToOne(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SubClass1 subClass121 = new SubClass1();
					subClass121.setId(121L);

					SubClass2 subClass221 = new SubClass2();
					subClass221.setId(221L);

					Container2 container21 = new Container2();
					container21.setId( 21L );
					container21.setSubClass12( subClass121 );
					container21.setSubClass22( subClass221 );
					subClass121.setManyToOneContainer( container21 );
					subClass221.setManyToOneContainer( container21 );

					session.persist( container21 );

					SubClass2 subClass222 = new SubClass2();
					subClass222.setId(222L);

					Container2 container22 = new Container2();
					container22.setId( 22L );
					container22.setSubClass22( subClass222 );
					subClass222.setManyToOneContainer( container22 );

					session.persist( container22 );
				}
		);
		scope.inTransaction(
				session -> {
					List<SubClass1> result1 = session.createSelectionQuery( "from SubClass1", SubClass1.class ).getResultList();
					assertEquals( 1, result1.size() );
					assertEquals( 121L, result1.get(0).getId() );

					List<SubClass2> result2 = session.createSelectionQuery( "from SubClass2 sc order by sc.id", SubClass2.class ).getResultList();
					assertEquals( 2, result2.size() );
					assertEquals( 221L, result2.get(0).getId() );
					assertEquals( 222L, result2.get(1).getId() );

					List<Container2> result3 = session.createSelectionQuery( "from Container2 c order by c.id", Container2.class ).getResultList();
					assertEquals( 2, result3.size() );
					assertEquals( 21L, result3.get(0).getId() );
					assertEquals( 22L, result3.get(1).getId() );

					SubClass1 sc1 = session.find( SubClass1.class, 121L );
					assertEquals( 21L, sc1.getManyToOneContainer().getId() );

					SubClass2 sc2 = session.find( SubClass2.class, 222L );
					assertEquals( 22L, sc2.getManyToOneContainer().getId() );

					Container2 c2 = session.find( Container2.class, 21L );
					assertEquals( 121L, c2.getSubClass12().getId() );
					assertEquals( 221L, c2.getSubClass22().getId() );

					c2 = session.find( Container2.class, 22L );
					assertEquals( 222L, c2.getSubClass22().getId() );
					assertNull( c2.getSubClass12() );
				}
		);

		tearDown( scope );

		scope.inTransaction(
				session -> {
					SubClass1 subClass12 = new SubClass1();
					subClass12.setId(12L);

					Container2 container = new Container2();
					container.setId( 2L );
					container.setSubClass12( subClass12 );
					subClass12.setManyToOneContainer( container );

					session.persist( container );
				}
		);
		scope.inTransaction(
				session -> {
					List<SubClass1> result1 = session.createSelectionQuery( "from SubClass1", SubClass1.class ).getResultList();
					assertEquals( 1, result1.size() );
					assertEquals( 12L, result1.get(0).getId() );
					List<SubClass2> result2 = session.createSelectionQuery( "from SubClass2", SubClass2.class ).getResultList();
					assertEquals( 0, result2.size() );
				}
		);

		tearDown( scope );

		scope.inTransaction(
				session -> {
					SubClass2 subClass22 = new SubClass2();
					subClass22.setId(22L);

					Container2 container = new Container2();
					container.setId( 2L );
					container.setSubClass22( subClass22 );
					subClass22.setManyToOneContainer( container );

					session.persist( container );
				}
		);
		scope.inTransaction(
				session -> {
					List<SubClass1> result1 = session.createSelectionQuery( "from SubClass1", SubClass1.class ).getResultList();
					assertEquals( 0, result1.size() );
					List<SubClass2> result2 = session.createSelectionQuery( "from SubClass2", SubClass2.class ).getResultList();
					assertEquals( 1, result2.size() );
					assertEquals( 22L, result2.get(0).getId() );
				}
		);

	}

	@Entity(name = "BaseClass")
	@DiscriminatorColumn(name = "BASE_TYPE")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class BaseClass {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "oneToOneContainer_id")
		private Container1 oneToOneContainer;

		@ManyToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "manyToOneContainer_id")
		private Container2 manyToOneContainer;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void set1To1Container(Container1 oneToOneContainer) {
			this.oneToOneContainer = oneToOneContainer;
		}

		public Container2 getManyToOneContainer() {
			return manyToOneContainer;
		}

		public void setManyToOneContainer(Container2 manyToOneContainer) {
			this.manyToOneContainer = manyToOneContainer;
		}
	}

	@Entity(name = "SubClass1")
	@DiscriminatorValue(value = "SUB1")
	public static class SubClass1 extends BaseClass {
	}

	@Entity(name = "SubClass2")
	@DiscriminatorValue(value = "SUB2")
	public static class SubClass2 extends BaseClass {
	}

	@Entity(name = "Container1")
	public static class Container1 {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "oneToOneContainer", orphanRemoval = true)
		private SubClass1 subClass11;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "oneToOneContainer", orphanRemoval = true)
		private SubClass2 subClass21;

		public void setId(Long id) {
			this.id = id;
		}

		public void setSubClass11(SubClass1 subClass11) {
			this.subClass11 = subClass11;
		}

		public void setSubClass21(SubClass2 subClass21) {
			this.subClass21 = subClass21;
		}
	}

	@Entity(name = "Container2")
	public static class Container2 {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "manyToOneContainer", orphanRemoval = true)
		private SubClass1 subClass12;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "manyToOneContainer", orphanRemoval = true)
		private SubClass2 subClass22;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public SubClass1 getSubClass12() {
			return subClass12;
		}

		public SubClass2 getSubClass22() {
			return subClass22;
		}

		public void setSubClass12(SubClass1 subClass12) {
			this.subClass12 = subClass12;
		}

		public void setSubClass22(SubClass2 subClass22) {
			this.subClass22 = subClass22;
		}
	}

}
