/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.join;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Jan Schatteman
 */
@TestForIssue( jiraKey = "HHH-16435")
@DomainModel(
		annotatedClasses = {
				JoinWithSingleTableInheritanceTest.AbstractSuperClass.class,
				JoinWithSingleTableInheritanceTest.ChildEntityA.class,
				JoinWithSingleTableInheritanceTest.ChildEntityB.class,
				JoinWithSingleTableInheritanceTest.RootOne.class
		}
)
@SessionFactory
public class JoinWithSingleTableInheritanceTest {

	@AfterEach
	public void cleanup( SessionFactoryScope scope ) {
		scope.inTransaction(
				s -> {
					s.createMutationQuery( "delete from RootOne" ).executeUpdate();
					s.createMutationQuery( "delete from AbstractSuperClass" ).executeUpdate();
				}
		);
	}

	@Test
	public void testLeftJoinOnSingleTableInheritance(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> s.persist( new RootOne(1) )
		);

		scope.inTransaction(
				s -> {
					List<RootOne> l = s.createSelectionQuery( "select r from RootOne r left join ChildEntityA ce on ce.id = r.someOtherId", RootOne.class ).list();
					assertEquals( 1, l.size() );
				}
		);
	}

	@Test
	public void testLeftJoinOnSingleTableInheritance2(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.persist(new ChildEntityA( 11 ));
					s.persist(new ChildEntityB( 21 ));
					RootOne r1 = new RootOne(1);
					r1.setSomeOtherId( 11 );
					s.persist( r1 );
					RootOne r2 = new RootOne(2);
					r2.setSomeOtherId( 21 );
					s.persist( r2 );
				}
		);

		scope.inTransaction(
				s -> {
					List<Object[]> l = s.createSelectionQuery( "select r.id, r.someOtherId, ce.id, ce.disc_col from RootOne r left join ChildEntityA ce on ce.id = r.someOtherId order by r.id", Object[].class ).list();
					assertEquals( 2, l.size() );
					Object[] r1 = l.get( 0 );
					assertEquals( 1, (int) r1[0] );
					assertEquals( 11, (int) r1[1] );
					assertEquals( 11, (int) r1[2] );
					assertEquals( 1, (int) r1[3] );
					// r2 has to be there, but shouldn't have any data w/ respect to ChildEntityB
					Object[] r2 = l.get( 1 );
					assertEquals( 2, (int) r2[0] );
					assertEquals( 21, (int) r2[1] );
					assertNull( r2[2] );
					assertNull( r2[3] );
				}
		);
	}

	@Test
	public void testCrossJoinOnSingleTableInheritance(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.persist(new ChildEntityA( 11 ));
					s.persist(new ChildEntityA( 12 ));
					s.persist(new ChildEntityB( 21 ));
					RootOne r1 = new RootOne(1);
					r1.setSomeOtherId( 11 );
					s.persist( r1 );
					RootOne r2 = new RootOne(2);
					r2.setSomeOtherId( 12 );
					s.persist( r2 );
					RootOne r3 = new RootOne(3);
					r3.setSomeOtherId( 21 );
					s.persist( r3 );
				}
		);

		scope.inTransaction(
				s -> {
					List<Object[]> l = s.createSelectionQuery( "select r.id, r.someOtherId from RootOne r join ChildEntityA", Object[].class ).list();
					assertEquals( 6, l.size() );
				}
		);
	}

	@Test
	public void testRightJoinOnSingleTableInheritance(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.persist(new ChildEntityA( 11 ));
					s.persist(new ChildEntityA( 12 ));
					s.persist(new ChildEntityB( 21 ));
					RootOne r1 = new RootOne(1);
					r1.setSomeOtherId( 11 );
					s.persist( r1 );
					RootOne r2 = new RootOne(2);
					r2.setSomeOtherId( 11 );
					s.persist( r2 );
					RootOne r3 = new RootOne(3);
					r3.setSomeOtherId( 21 );
					s.persist( r3 );
				}
		);

		scope.inTransaction(
				s -> {
					List<Object[]> l = s.createSelectionQuery( "select r.id, r.someOtherId, ce.id, ce.disc_col from RootOne r right join ChildEntityA ce on ce.id = r.someOtherId order by ce.id, r.id", Object[].class ).list();
					assertEquals( 3, l.size() );
					Object[] r1 = l.get( 0 );
					assertEquals( 1, (int) r1[0] );
					assertEquals( 11, (int) r1[1] );
					assertEquals( 11, (int) r1[2] );
					assertEquals( 1, (int) r1[3] );
					Object[] r2 = l.get( 1 );
					assertEquals( 2, (int) r2[0] );
					assertEquals( 11, (int) r2[1] );
					assertEquals( 11, (int) r2[2] );
					assertEquals( 1, (int) r2[3] );
					Object[] r3 = l.get( 2 );
					assertNull( r3[0] );
					assertNull( r3[1] );
					assertEquals( 12, (int) r3[2] );
					assertEquals( 1, (int) r3[3] );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFullJoin.class)
	public void testFullJoinOnSingleTableInheritance(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.persist(new ChildEntityA( 11 ));
					s.persist(new ChildEntityA( 12 ));
					s.persist(new ChildEntityB( 21 ));
					s.persist(new ChildEntityB( 22 ));
					RootOne r1 = new RootOne(1);
					r1.setSomeOtherId( 11 );
					s.persist( r1 );
					RootOne r2 = new RootOne(2);
					r2.setSomeOtherId( 11 );
					s.persist( r2 );
					RootOne r3 = new RootOne(3);
					r3.setSomeOtherId( 21 );
					s.persist( r3 );
					RootOne r4 = new RootOne(4);
					s.persist( r4 );
				}
		);

		scope.inTransaction(
				s -> {
					List<Object[]> l = s.createSelectionQuery( "select r.id, r.someOtherId from RootOne r full join ChildEntityA ce order by ce.id, r.id", Object[].class ).list();
					assertEquals( 10, l.size() );
				}
		);
	}

	@Entity(name = "AbstractSuperClass")
	@DiscriminatorColumn(name = "disc_col", discriminatorType = DiscriminatorType.INTEGER)
	public static abstract class AbstractSuperClass {
		@Id
		private Integer id;

		@Column(insertable = false, updatable = false)
		private Integer disc_col;

		public AbstractSuperClass(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "ChildEntityA")
	@DiscriminatorValue("1")
	public static class ChildEntityA extends AbstractSuperClass {
		public ChildEntityA(Integer id) {
			super( id );
		}
	}

	@Entity(name = "ChildEntityB")
	@DiscriminatorValue("2")
	public static class ChildEntityB extends AbstractSuperClass {
		public ChildEntityB(Integer id) {
			super( id );
		}
	}

	@Entity(name = "RootOne")
	public static class RootOne {
		@Id
		Integer id;
		Integer someOtherId;

		public RootOne() {
		}

		public RootOne(Integer id) {
			this.id = id;
		}

		public Integer getSomeOtherId() {
			return someOtherId;
		}

		public void setSomeOtherId(Integer someOtherId) {
			this.someOtherId = someOtherId;
		}
	}

}
