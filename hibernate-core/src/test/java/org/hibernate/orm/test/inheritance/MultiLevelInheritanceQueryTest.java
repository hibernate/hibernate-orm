/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MultiLevelInheritanceQueryTest.AbstractRootEntity.class,
		MultiLevelInheritanceQueryTest.AbstractSuperclass.class,
		MultiLevelInheritanceQueryTest.ChildOneEntity.class,
		MultiLevelInheritanceQueryTest.ChildTwoEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17292" )
public class MultiLevelInheritanceQueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new ChildTwoEntity( 1, "superclass_1", "child_one_1", "child_two_1" ) );
			session.persist( new ChildTwoEntity( 2, "superclass_2", "child_one_2", "child_two_2" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from AbstractRootEntity" ).executeUpdate() );
	}

	@Test
	public void testSelectionQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( PersistentClass pc : scope.getMetadataImplementor().getEntityBindings() ) {
				// Run test for each class in the inheritance tree
				final Class<?> entityClass = pc.getMappedClass();
				executeSelectionQuery( session, entityClass, "baseProp = 1" );
				executeSelectionQuery( session, entityClass, "mappedSuperclassProp = 'superclass_1'" );
				executeSelectionQuery( session, entityClass, "childOneProp = 'child_one_1'" );
				executeSelectionQuery( session, entityClass, "childTwoProp = 'child_two_1'" );
			}
		} );
	}

	private void executeSelectionQuery(SessionImplementor session, Class<?> entityClass, String whereClause) {
		assertThat( session.createSelectionQuery(
				String.format( "from %s where %s", entityClass.getName(), whereClause ),
				entityClass
		).getResultList() ).hasSize( 1 );
	}

	@Test
	public void testMutationQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( PersistentClass pc : scope.getMetadataImplementor().getEntityBindings() ) {
				// Run test for each class in the inheritance tree
				final Class<?> entityClass = pc.getMappedClass();
				executeMutationQuery( session, entityClass, "baseProp = 2" );
				executeMutationQuery( session, entityClass, "mappedSuperclassProp = 'superclass_2'" );
				executeMutationQuery( session, entityClass, "childOneProp = 'child_one_2'" );
				executeMutationQuery( session, entityClass, "childTwoProp = 'child_two_2'" );
			}
		} );
	}

	private void executeMutationQuery(SessionImplementor session, Class<?> entityClass, String whereClause) {
		assertThat( session.createMutationQuery( String.format(
				"update %s set baseProp = 2 where %s",
				entityClass.getSimpleName(),
				whereClause
		) ).executeUpdate() ).isEqualTo( 1 );
	}

	@Entity( name = "AbstractRootEntity" )
	@Table( name = "entity" )
	@DiscriminatorColumn( name = "disc_col", discriminatorType = DiscriminatorType.INTEGER )
	public abstract static class AbstractRootEntity {
		@Id
		@GeneratedValue
		private Long id;

		private Integer baseProp;

		public AbstractRootEntity() {
		}

		public AbstractRootEntity(Integer baseProp) {
			this.baseProp = baseProp;
		}
	}

	@MappedSuperclass
	public abstract static class AbstractSuperclass extends AbstractRootEntity {
		private String mappedSuperclassProp;

		public AbstractSuperclass() {
		}

		public AbstractSuperclass(Integer baseProp, String mappedSuperclassProp) {
			super( baseProp );
			this.mappedSuperclassProp = mappedSuperclassProp;
		}
	}

	@Entity( name = "ChildOneEntity" )
	@DiscriminatorValue( "1" )
	public static class ChildOneEntity extends AbstractSuperclass {
		private String childOneProp;

		public ChildOneEntity() {
		}

		public ChildOneEntity(Integer baseProp, String mappedSuperclassProp, String childOneProp) {
			super( baseProp, mappedSuperclassProp );
			this.childOneProp = childOneProp;
		}
	}

	@Entity( name = "ChildTwoEntity" )
	@DiscriminatorValue( "2" )
	public static class ChildTwoEntity extends ChildOneEntity {
		private String childTwoProp;

		public ChildTwoEntity() {
		}

		public ChildTwoEntity(Integer baseProp, String mappedSuperclassProp, String childOneProp, String childTwoProp) {
			super( baseProp, mappedSuperclassProp, childOneProp );
			this.childTwoProp = childTwoProp;
		}
	}
}
