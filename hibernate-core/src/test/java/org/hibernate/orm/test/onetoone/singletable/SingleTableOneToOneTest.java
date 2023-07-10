/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.onetoone.singletable;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
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
@JiraKey( value = "HHH-16916")
public class SingleTableOneToOneTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from SubClass2" ).executeUpdate();
					session.createMutationQuery( "delete from SubClass1" ).executeUpdate();
					session.createMutationQuery( "delete from Container1" ).executeUpdate();
					session.createMutationQuery( "delete from Container2" ).executeUpdate();
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Sybase ignores unique constraints on nullable columns")
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
					SubClass1 subClass12 = new SubClass1();
					subClass12.setId(12L);

					SubClass2 subClass22 = new SubClass2();
					subClass22.setId(22L);

					Container2 container = new Container2();
					container.setId( 2L );
					container.setSubClass12( subClass12 );
					container.setSubClass22( subClass22 );
					subClass12.setManyToOneContainer( container );
					subClass22.setManyToOneContainer( container );

					session.persist( container );
				}
		);
		scope.inTransaction(
				session -> {
					assertEquals( 1L, session.createSelectionQuery( "select count(*) from SubClass1", Long.class ).getSingleResult() );
					assertEquals( 1L, session.createSelectionQuery( "select count(*) from SubClass2", Long.class ).getSingleResult() );
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

		public void setId(Long id) {
			this.id = id;
		}

		public void setSubClass12(SubClass1 subClass12) {
			this.subClass12 = subClass12;
		}

		public void setSubClass22(SubClass2 subClass22) {
			this.subClass22 = subClass22;
		}
	}

}
