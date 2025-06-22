/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		JoinedInheritanceToOneSameHierarchyTest.MasterEntity.class,
		JoinedInheritanceToOneSameHierarchyTest.ConfigEntity.class,
		JoinedInheritanceToOneSameHierarchyTest.ContextEntity.class,
		JoinedInheritanceToOneSameHierarchyTest.DocumentEntity.class,
		JoinedInheritanceToOneSameHierarchyTest.TypeEntity.class,
} )
@SessionFactory
public class JoinedInheritanceToOneSameHierarchyTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TypeEntity type = new TypeEntity();
			type.setId( 1L );
			type.setName( "type" );
			session.persist( type );

			final ContextEntity context = new ContextEntity();
			context.setId( 2L );
			context.setType( type );
			session.persist( context );

			final ConfigEntity config = new ConfigEntity();
			config.setId( 3L );
			session.persist( config );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<ConfigEntity> resultList = session.createQuery(
					"from ConfigEntity c order by c.id",
					ConfigEntity.class
			).getResultList();
			assertThat( resultList.get( 0 ) ).isInstanceOf( ContextEntity.class );
			assertThat( ( (ContextEntity) resultList.get( 0 ) ).getType().getName() ).isEqualTo( "type" );
			assertThat( resultList.get( 1 ) ).isInstanceOf( ConfigEntity.class );
		} );
	}

	@Test
	public void testQueryAndJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<ConfigEntity> resultList = session.createQuery(
					"from ConfigEntity c left join treat(c as ContextEntity).type order by c.id",
					ConfigEntity.class
			).getResultList();
			assertThat( resultList.get( 0 ) ).isInstanceOf( ContextEntity.class );
			assertThat( ( (ContextEntity) resultList.get( 0 ) ).getType().getName() ).isEqualTo( "type" );
			assertThat( resultList.get( 1 ) ).isInstanceOf( ConfigEntity.class );
		} );
	}

	@Test
	public void testQueryAndJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<ConfigEntity> resultList = session.createQuery(
					"from ConfigEntity c left join fetch treat(c as ContextEntity).type order by c.id",
					ConfigEntity.class
			).getResultList();
			assertThat( resultList.get( 0 ) ).isInstanceOf( ContextEntity.class );
			assertThat( ( (ContextEntity) resultList.get( 0 ) ).getType().getName() ).isEqualTo( "type" );
			assertThat( resultList.get( 1 ) ).isInstanceOf( ConfigEntity.class );
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ConfigEntity config = session.find( ConfigEntity.class, 2L );
			assertThat( config ).isInstanceOf( ContextEntity.class );
			assertThat( ( (ContextEntity) config ).getType().getName() ).isEqualTo( "type" );
		} );
	}

	@Entity( name = "MasterEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	static class MasterEntity {
		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "ConfigEntity" )
	static class ConfigEntity extends MasterEntity {
	}

	@Entity( name = "ContextEntity" )
	static class ContextEntity extends ConfigEntity {
		@ManyToOne
		private TypeEntity type;

		public TypeEntity getType() {
			return type;
		}

		public void setType(TypeEntity type) {
			this.type = type;
		}
	}

	@Entity( name = "DocumentEntity" )
	static class DocumentEntity extends MasterEntity {
	}

	@Entity( name = "TypeEntity" )
	static class TypeEntity extends DocumentEntity {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
