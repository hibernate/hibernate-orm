/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.join;

import java.util.List;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
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
					s.createMutationQuery( "delete from AbstractSuperClass" ).executeUpdate();
					s.createMutationQuery( "delete from RootOne" ).executeUpdate();
				}
		);
	}

	@Test
	public void testLeftJoinOnSingleTableInheritance(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> s.persist( new RootOne() )
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
					RootOne r1 = new RootOne();
					r1.setSomeOtherId( 11 );
					s.persist( r1 );
					RootOne r2 = new RootOne();
					r2.setSomeOtherId( 21 );
					s.persist( r2 );
				}
		);

		scope.inTransaction(
				s -> {
					List<Object[]> l = s.createSelectionQuery( "select r.id, r.someOtherId, ce.id from RootOne r left join ChildEntityA ce on ce.id = r.someOtherId order by r.id", Object[].class ).list();
					assertEquals( 2, l.size() );
					Object[] r1 = l.get( 0 );
					assertEquals( (int) r1[0], 1 );
					assertEquals( (int) r1[1], 11 );
					assertEquals( (int) r1[2], 11 );
					// r2 has to be there, but shouldn't have any data w/ respect to ChildEntityB
					Object[] r2 = l.get( 1 );
					assertEquals( (int) r2[0], 2 );
					assertEquals( (int) r2[1], 21 );
					assertNull( r2[2] );

				}
		);
	}

	@Entity(name = "AbstractSuperClass")
	@DiscriminatorColumn(name = "DISC_COL", discriminatorType = DiscriminatorType.INTEGER)
	public static abstract class AbstractSuperClass {
		@Id
		private Integer id;

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
		@GeneratedValue
		Integer id;
		Integer someOtherId;

		public Integer getSomeOtherId() {
			return someOtherId;
		}

		public void setSomeOtherId(Integer someOtherId) {
			this.someOtherId = someOtherId;
		}
	}

}
