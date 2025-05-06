/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.where;

import java.util.Set;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.community.dialect.AltibaseDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@DomainModel( annotatedClasses = {
		DiscriminatorWhereTest.EntityA.class,
		DiscriminatorWhereTest.EntityB.class,
		DiscriminatorWhereTest.EntityC.class
})
@SessionFactory
@JiraKey( "https://hibernate.atlassian.net/browse/HHH-14977" )
@SkipForDialect( dialectClass = AltibaseDialect.class, reason = "'TYPE' is not escaped even though autoQuoteKeywords is enabled")
public class DiscriminatorWhereTest {
	@Test
	public void testAddDiscriminatedEntityToCollectionWithWhere(SessionFactoryScope scope) {
		// Initially save EntityA with an empty collection
		final Integer id = scope.fromTransaction( (session) -> {
			final EntityA entityA = new EntityA();
			entityA.setName( "a" );
			session.persist( entityA );
			return entityA.getId();
		} );

		// Fetch EntityA and add a new EntityC to its collection.
		// The collection is annotated with @Where("TYPE = 'C'")
		scope.inTransaction( (session) -> {
			final EntityA entityA = session.find( EntityA.class, id );
			final EntityC entityC = new EntityC();
			entityC.setName( "c" );

			// `#add` triggers the error as it tries to fetch the Set
			entityA.getAllMyC().add( entityC );

			session.persist( entityC );
			// todo: this merge fails with a SQL exception
			//  Syntax error in SQL statement "SELECT A1_0.ALLC,A1_0.ID,A1_0.NAME FROM B_TAB A1_0 WHERE A1_0.ALLC=? AND (A1_0.TYPE = 'C') A1_0[*].TYPE='C' ";
			session.merge( entityA );
		} );
	}

	@Entity(name = "EnttiyA")
	@Table(name = "a_tab")
	public static class EntityA {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		@OneToMany
		@JoinColumn(name = "allC")
		@SQLRestriction("type = 'C'")
		private Set<EntityC> allMyC;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<EntityC> getAllMyC() {
			return allMyC;
		}

		public void setAllMyC(Set<EntityC> allMyC) {
			this.allMyC = allMyC;
		}
	}

	@Entity(name = "EntityB")
	@Table(name = "b_tab")
	@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
	@DiscriminatorValue( value = "B")
	public static class EntityB {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityC")
	@DiscriminatorValue(value = "C")
	public static class EntityC extends EntityB {

	}
}
