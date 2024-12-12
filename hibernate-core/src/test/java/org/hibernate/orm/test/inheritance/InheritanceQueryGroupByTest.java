/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupport;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory( useCollectingStatementInspector = true )
@DomainModel( annotatedClasses = {
		InheritanceQueryGroupByTest.Parent.class,
		InheritanceQueryGroupByTest.SingleTableParent.class,
		InheritanceQueryGroupByTest.SingleTableChildOne.class,
		InheritanceQueryGroupByTest.SingleTableChildTwo.class,
		InheritanceQueryGroupByTest.JoinedParent.class,
		InheritanceQueryGroupByTest.JoinedChildOne.class,
		InheritanceQueryGroupByTest.JoinedChildTwo.class,
		InheritanceQueryGroupByTest.TPCParent.class,
		InheritanceQueryGroupByTest.TPCChildOne.class,
		InheritanceQueryGroupByTest.TPCChildTwo.class,
		InheritanceQueryGroupByTest.MyEntity.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16349" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16773" )
public class InheritanceQueryGroupByTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SingleTableChildOne st1 = new SingleTableChildOne();
			st1.setName( "single_table_child_one" );
			session.persist( st1 );
			final JoinedChildOne j1 = new JoinedChildOne();
			j1.setName( "joined_child_one" );
			session.persist( j1 );
			final TPCChildOne tpc1 = new TPCChildOne();
			tpc1.setName( "tpc_child_one" );
			session.persist( tpc1 );
			session.persist( new MyEntity( 1, st1, j1, tpc1 ) );
			session.persist( new MyEntity( 2, st1, j1, tpc1 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from MyEntity" ).executeUpdate();
			session.createMutationQuery( "delete from " + Parent.class.getName() ).executeUpdate();
		} );
	}

	@Test
	public void testGroupBySingleTable(SessionFactoryScope scope) {
		testGroupBy( scope, "singleTableParent", SingleTableParent.class, "single_table_child_one", 1 );
	}

	@Test
	@SkipForDialect( dialectClass = InformixDialect.class , reason = "Informix does not support case expressions within the GROUP BY clause")
	public void testGroupByJoined(SessionFactoryScope scope) {
		testGroupBy( scope, "joinedParent", JoinedParent.class, "joined_child_one", 1 );
	}

	@Test
	public void testGroupByTPC(SessionFactoryScope scope) {
		testGroupBy( scope, "tpcParent", TPCParent.class, "tpc_child_one", 4 );
	}

	private void testGroupBy(
			SessionFactoryScope scope,
			String parentProp,
			Class<?> parentEntityClass,
			String parentName,
			int childPropCount) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final MyPojo myPojo = session.createQuery(
					"select new MyPojo(sum(e.amount), re) from MyEntity e join e." + parentProp + " re group by re",
					MyPojo.class
			).getSingleResult();
			assertThat( myPojo.getAmount() ).isEqualTo( 3L );
			assertThat( myPojo.getParent().getName() ).isEqualTo( parentName );
			final EntityMappingType entityMappingType = scope.getSessionFactory()
					.getMappingMetamodel()
					.findEntityDescriptor( parentEntityClass );
			final int expectedCount = supportsFunctionalDependency( scope, entityMappingType ) ?
					childPropCount :
					childPropCount + 1;
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_one_col", expectedCount );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_two_col", expectedCount );
		} );
	}

	@Test
	public void testGroupByNotSelectedSingleTable(SessionFactoryScope scope) {
		testGroupByNotSelected( scope, "singleTableParent", "single_table_parent_id", 0 );
	}

	@Test
	public void testGroupByNotSelectedJoined(SessionFactoryScope scope) {
		testGroupByNotSelected( scope, "joinedParent", "joined_parent_id", 0 );
	}

	@Test
	public void testGroupByNotSelectedTPC(SessionFactoryScope scope) {
		testGroupByNotSelected( scope, "tpcParent", "tpc_parent_id", 3 );
	}

	private void testGroupByNotSelected(
			SessionFactoryScope scope,
			String parentProp,
			String parentFkName,
			int childPropCount) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Long sum = session.createQuery(
					String.format( "select sum(e.amount) from MyEntity e join e.%s re group by re", parentProp ),
					Long.class
			).getSingleResult();
			assertThat( sum ).isEqualTo( 3L );
			// Association is joined, so every use of the join alias will make use of target table columns
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, parentFkName, 1 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_one_col", childPropCount );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_two_col", childPropCount );
		} );
	}

	@Test
	public void testGroupByAndOrderBySingleTable(SessionFactoryScope scope) {
		testGroupByAndOrderBy( scope, "singleTableParent", SingleTableParent.class, "single_table_child_one", 1 );
	}

	@Test
	@SkipForDialect( dialectClass = InformixDialect.class , reason = "Informix does not support case expressions within the GROUP BY clause")
	public void testGroupByAndOrderByJoined(SessionFactoryScope scope) {
		testGroupByAndOrderBy( scope, "joinedParent", JoinedParent.class, "joined_child_one", 1 );
	}

	@Test
	public void testGroupByAndOrderByTPC(SessionFactoryScope scope) {
		testGroupByAndOrderBy( scope, "tpcParent", TPCParent.class, "tpc_child_one", 4 );
	}

	private void testGroupByAndOrderBy(
			SessionFactoryScope scope,
			String parentProp,
			Class<?> parentEntityClass,
			String parentName,
			int childPropCount) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final MyPojo myPojo = session.createQuery(
					String.format(
							"select new %s(sum(e.amount), re) from MyEntity e join e.%s re group by re order by re",
							MyPojo.class.getName(),
							parentProp
					),
					MyPojo.class
			).getSingleResult();
			assertThat( myPojo.getAmount() ).isEqualTo( 3L );
			assertThat( myPojo.getParent().getName() ).isEqualTo( parentName );
			final EntityMappingType entityMappingType = scope.getSessionFactory()
					.getMappingMetamodel()
					.findEntityDescriptor( parentEntityClass );
			final int expectedCount = supportsFunctionalDependency( scope, entityMappingType ) ?
					childPropCount :
					childPropCount + 2;
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_one_col", expectedCount );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_two_col", expectedCount );
		} );
	}

	@Test
	public void testGroupByAndOrderByNotSelectedSingleTable(SessionFactoryScope scope) {
		testGroupByAndOrderByNotSelected( scope, "singleTableParent", "single_table_parent_id", 0 );
	}

	@Test
	public void testGroupByAndOrderByNotSelectedJoined(SessionFactoryScope scope) {
		testGroupByAndOrderByNotSelected( scope, "joinedParent", "joined_parent_id", 0 );
	}

	@Test
	public void testGroupByAndOrderByNotSelectedTPC(SessionFactoryScope scope) {
		testGroupByAndOrderByNotSelected( scope, "tpcParent", "tpc_parent_id", 3 );
	}

	private void testGroupByAndOrderByNotSelected(
			SessionFactoryScope scope,
			String parentProp,
			String parentFkName,
			int childPropCount) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Long sum = session.createQuery(
					String.format(
							"select sum(e.amount) from MyEntity e join e.%s re group by re order by re",
							parentProp
					),
					Long.class
			).getSingleResult();
			assertThat( sum ).isEqualTo( 3L );
			// Association is joined, so every use of the join alias will make use of target table columns
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, parentFkName, 1 );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_one_col", childPropCount );
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_two_col", childPropCount );
		} );
	}

	private static Dialect getDialect(SessionFactoryScope scope) {
		return scope.getSessionFactory().getJdbcServices().getDialect();
	}

	private static boolean supportsFunctionalDependency(
			SessionFactoryScope scope,
			EntityMappingType entityMappingType) {
		final FunctionalDependencyAnalysisSupport analysisSupport = scope.getSessionFactory()
				.getJdbcServices()
				.getDialect()
				.getFunctionalDependencyAnalysisSupport();
		if ( analysisSupport.supportsAnalysis() ) {
			if ( entityMappingType.getSqmMultiTableMutationStrategy() == null ) {
				return true;
			}
			else {
				return analysisSupport.supportsTableGroups() && ( analysisSupport.supportsConstants() ||
						// Union entity persisters use a literal 'clazz_' column as a discriminator
						// that breaks functional dependency for dialects that don't support constants
						!( entityMappingType.getEntityPersister() instanceof UnionSubclassEntityPersister ) );
			}
		}
		return false;
	}

	@MappedSuperclass
	public abstract static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "SingleTableParent" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "disc_col", discriminatorType = DiscriminatorType.INTEGER )
	public static class SingleTableParent extends Parent {
	}

	@Entity( name = "SingleTableChildOne" )
	@DiscriminatorValue( "1" )
	public static class SingleTableChildOne extends SingleTableParent {
		@Column( name = "child_one_col" )
		private Integer childOneProp;
	}

	@Entity( name = "SingleTableChildTwo" )
	@DiscriminatorValue( "2" )
	public static class SingleTableChildTwo extends SingleTableParent {
		@Column( name = "child_two_col" )
		private Integer childTwoProp;
	}

	@Entity( name = "JoinedParent" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class JoinedParent extends Parent {
	}

	@Entity( name = "JoinedChildOne" )
	public static class JoinedChildOne extends JoinedParent {
		@Column( name = "child_one_col" )
		private Integer childOneProp;
	}

	@Entity( name = "JoinedChildTwo" )
	public static class JoinedChildTwo extends JoinedParent {
		@Column( name = "child_two_col" )
		private Integer childTwoProp;
	}

	@Entity( name = "TPCParent" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	public static class TPCParent extends Parent {
	}

	@Entity( name = "TPCChildOne" )
	public static class TPCChildOne extends TPCParent {
		@Column( name = "child_one_col" )
		private Integer childOneProp;
	}

	@Entity( name = "TPCChildTwo" )
	public static class TPCChildTwo extends TPCParent {
		@Column( name = "child_two_col" )
		private Integer childTwoProp;
	}

	@Entity( name = "MyEntity" )
	public static class MyEntity {
		@Id
		@GeneratedValue
		private Long id;

		private Integer amount;

		@ManyToOne
		@JoinColumn( name = "single_table_parent_id" )
		private SingleTableParent singleTableParent;

		@ManyToOne
		@JoinColumn( name = "joined_parent_id" )
		private JoinedParent joinedParent;

		@ManyToOne
		@JoinColumn( name = "tpc_parent_id" )
		private TPCParent tpcParent;

		public MyEntity() {
		}

		public MyEntity(
				Integer amount,
				SingleTableParent singleTableParent,
				JoinedParent joinedParent,
				TPCParent tpcParent) {
			this.amount = amount;
			this.singleTableParent = singleTableParent;
			this.joinedParent = joinedParent;
			this.tpcParent = tpcParent;
		}
	}

	public static class MyPojo {
		private final Long amount;

		private final Parent parent;

		public MyPojo(Long amount, Parent parent) {
			this.amount = amount;
			this.parent = parent;
		}

		public Long getAmount() {
			return amount;
		}

		public Parent getParent() {
			return parent;
		}
	}
}
