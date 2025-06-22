/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.Set;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.Assert.assertEquals;


/**
 * Provides test cases for the following {@link OneToMany} mapping with {@linkplain SQLRestriction}
 *
 * @author Chris Cranford
 */
@JiraKey("HHH-9432")
@SkipForDialect( dialectClass = AltibaseDialect.class, reason = "'TYPE' is not escaped even though autoQuoteKeywords is enabled")
public class BasicWhereTest extends BaseEnversJPAFunctionalTestCase {
	private Integer aId;
	private Integer xId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				EntityA.class,
				EntityB.class,
				EntityC.class,
				EntityX.class,
				EntityY.class,
				EntityZ.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		aId = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final EntityA a = new EntityA();
			a.setName( "a" );
			entityManager.persist( a );
			return a.getId();
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final EntityA a = entityManager.find( EntityA.class, aId );
			final EntityC c = new EntityC();
			c.setName( "c" );
			a.getAllMyC().add( c );
			entityManager.persist( c );
			entityManager.merge( a );
		} );

		xId = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final EntityX x = new EntityX();
			x.setName( "x" );
			entityManager.persist( x );
			return x.getId();
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final EntityX x = entityManager.find( EntityX.class, xId );
			final EntityZ z = new EntityZ();
			z.setName( "z" );
			x.getAllMyZ().add( z );
			entityManager.persist( z );
			entityManager.merge( x );
		} );
	}

	@Test
	public void testWherePredicateWithAuditJoinTable() {
		final EntityA a = getAuditReader().find( EntityA.class, aId, 2 );
		assertEquals( 1, a.getAllMyC().size() );
	}

	@Test
	public void testWherePredicateWithoutAuditJoinTable() {
		final EntityX x = getAuditReader().find( EntityX.class, xId, 4 );
		assertEquals( 1, x.getAllMyZ().size() );
	}

	@Audited
	@Entity(name = "EntityA")
	@Table(name = "a_tab")
	public static class EntityA {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		@OneToMany
		@JoinTable(joinColumns = @JoinColumn(name = "allC"))
		@SQLRestriction("type = 'C'")
		@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
		@AuditJoinTable(name = "A_C_AUD")
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

	@Audited
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

	@Audited
	@Entity(name = "EntityX")
	@Table(name = "x_tab")
	public static class EntityX {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		@OneToMany
		@SQLRestriction("type = 'Z'")
		@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
		private Set<EntityZ> allMyZ;

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

		public Set<EntityZ> getAllMyZ() {
			return allMyZ;
		}

		public void setAllMyZ(Set<EntityZ> allMyZ) {
			this.allMyZ = allMyZ;
		}
	}

	@Audited
	@Entity(name = "EntityY")
	@Table(name = "y_tab")
	@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
	@DiscriminatorValue( value = "Y")
	public static class EntityY {
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

	@Entity(name = "EntityZ")
	@DiscriminatorValue(value = "Z")
	public static class EntityZ extends EntityY {

	}
}
