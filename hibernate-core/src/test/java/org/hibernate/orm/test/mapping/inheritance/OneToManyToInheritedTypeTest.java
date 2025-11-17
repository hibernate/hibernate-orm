/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Laurent Almeras
 */
@DomainModel(
		annotatedClasses = {
				OneToManyToInheritedTypeTest.SuperType.class,
				OneToManyToInheritedTypeTest.TypeA.class,
				OneToManyToInheritedTypeTest.TypeB.class,
				OneToManyToInheritedTypeTest.LinkedEntity.class
		}
)
@ServiceRegistry
@SessionFactory
public class OneToManyToInheritedTypeTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SuperType superType = new SuperType( 1 );
					TypeB typeB = new TypeB( 2, "typeB" );
					TypeA typeA = new TypeA( 3, "typeA" );
					LinkedEntity entity = new LinkedEntity( 3 );
					entity.addSuperType( superType );
					entity.addSuperType( typeB );
					entity.addSuperType( typeA );
					session.persist( superType );
					session.persist( typeB );
					session.persist( typeA );
					session.persist( entity );
				}
		);
	}

	@Test
	public void basicTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					LinkedEntity entity = session.find( LinkedEntity.class, 3 );
					List<SuperType> superTypes = entity.getSuperTypes();
					assertThat( superTypes.size(), is( 3 ) );
					for ( SuperType superType : superTypes ) {
						if ( superType.getId() == 2 ) {
							assertThat( superType, instanceOf( TypeB.class ) );
							TypeB typeB = (TypeB) superType;
							assertThat( typeB.getTypeBName(), is( "typeB" ) );
						}
						if ( superType.getId() == 3 ) {
							assertThat( superType, instanceOf( TypeA.class ) );
							TypeA typeB = (TypeA) superType;
							assertThat( typeB.getTypeAName(), is( "typeA" ) );
						}
					}
				}
		);
	}

	@Entity(name = "SuperType")
	public static class SuperType {
		private Integer id;

		public SuperType() {
		}

		public SuperType(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "TypeA")
	public static class TypeA extends SuperType {
		private String typeAName;

		public TypeA() {
		}

		public TypeA(Integer id, String typeAName) {
			super( id );
			this.typeAName = typeAName;
		}

		public String getTypeAName() {
			return typeAName;
		}

		public void setTypeAName(String typeAName) {
			this.typeAName = typeAName;
		}
	}

	@Entity(name = "TypeB")
	public static class TypeB extends SuperType {
		private String typeBName;

		public TypeB() {
		}

		public TypeB(Integer id, String typeBName) {
			super( id );
			this.typeBName = typeBName;
		}

		public String getTypeBName() {
			return typeBName;
		}

		public void setTypeBName(String typeBName) {
			this.typeBName = typeBName;
		}
	}

	@Entity(name = "LinkedEntity")
	public static class LinkedEntity {
		private Integer id;
		private List<SuperType> superTypes;

		public LinkedEntity() {
		}

		public LinkedEntity(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@OneToMany
		public List<SuperType> getSuperTypes() {
			return superTypes;
		}

		public void setSuperTypes(List<SuperType> superTypes) {
			this.superTypes = superTypes;
		}

		public void addSuperType(SuperType superType) {
			if ( superTypes == null ) {
				superTypes = new ArrayList<>();
			}
			this.superTypes.add( superType );
		}
	}
}
