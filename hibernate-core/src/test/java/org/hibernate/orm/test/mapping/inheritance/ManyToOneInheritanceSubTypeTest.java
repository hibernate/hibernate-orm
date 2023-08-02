/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.inheritance;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Laurent Almeras
 */
@DomainModel( annotatedClasses = {
		ManyToOneInheritanceSubTypeTest.SuperType.class,
		ManyToOneInheritanceSubTypeTest.TypeA.class,
		ManyToOneInheritanceSubTypeTest.TypeB.class,
		ManyToOneInheritanceSubTypeTest.LinkedEntity.class
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16616" )
public class ManyToOneInheritanceSubTypeTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SuperType superType = new SuperType( 1 );
			final TypeB typeB = new TypeB( 2, "typeB" );
			final TypeA typeA = new TypeA( 3, "typeA" );
			final LinkedEntity entity = new LinkedEntity( 4 );
			entity.addTypeA( typeA );
			session.persist( superType );
			session.persist( typeB );
			session.persist( typeA );
			session.persist( entity );
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final LinkedEntity entity = session.find( LinkedEntity.class, 4 );
			assertThat( entity.getTypeAS() ).hasSize( 1 );
			inspector.assertExecutedCount( 2 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "disc_col", 1 );
		} );
	}

	@Test
	public void testJoinFetch(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final LinkedEntity entity = session.createQuery(
					"from LinkedEntity e left join fetch e.typeAS",
					LinkedEntity.class
			).getSingleResult();
			assertThat( entity.getTypeAS() ).hasSize( 1 );
			inspector.assertExecutedCount( 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
		} );
	}

	@Entity( name = "SuperType" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "disc_col" )
	public static class SuperType {
		@Id
		private Integer id;

		public SuperType() {
		}

		public SuperType(Integer id) {
			this.id = id;
		}
	}

	@Entity( name = "TypeA" )
	public static class TypeA extends SuperType {
		private String typeAName;

		public TypeA() {
		}

		public TypeA(Integer id, String typeAName) {
			super( id );
			this.typeAName = typeAName;
		}
	}

	@Entity( name = "TypeB" )
	public static class TypeB extends SuperType {
		private String typeBName;

		public TypeB() {
		}

		public TypeB(Integer id, String typeBName) {
			super( id );
			this.typeBName = typeBName;
		}
	}

	@Entity( name = "LinkedEntity" )
	public static class LinkedEntity {
		@Id
		private Integer id;

		@OneToMany
		@JoinColumn( name = "linked_id" )
		private List<TypeA> typeAS = new ArrayList<>();

		public LinkedEntity() {
		}

		public LinkedEntity(Integer id) {
			this.id = id;
		}

		public List<TypeA> getTypeAS() {
			return typeAS;
		}

		public void addTypeA(TypeA typeA) {
			this.typeAS.add( typeA );
		}
	}
}
