/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-13080")
@EnversTest
@Jpa(annotatedClasses = {
		DetachedCollectionChangeTest.Alert.class,
		DetachedCollectionChangeTest.RuleName.class
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DetachedCollectionChangeTest {
	@Audited
	@Entity(name = "Alert")
	public static class Alert {
		@Id
		@GeneratedValue
		private Integer id;
		@ManyToMany
		private List<RuleName> ruleNames = new ArrayList<>();
		@ElementCollection
		private List<String> names = new ArrayList<>();
		@ElementCollection
		private Set<CompositeName> composites = new HashSet<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<RuleName> getRuleNames() {
			return ruleNames;
		}

		public void setRuleNames(List<RuleName> ruleNames) {
			this.ruleNames = ruleNames;
		}

		public List<String> getNames() {
			return names;
		}

		public void setNames(List<String> names) {
			this.names = names;
		}

		public Set<CompositeName> getComposites() {
			return composites;
		}

		public void setComposites(Set<CompositeName> composites) {
			this.composites = composites;
		}
	}

	@Audited
	@Entity(name = "RuleName")
	public static class RuleName {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		public RuleName() {

		}

		public RuleName(String name) {
			this.name = name;
		}

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

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			RuleName ruleName = (RuleName) o;
			return Objects.equals( id, ruleName.id ) &&
				Objects.equals( name, ruleName.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name );
		}
	}

	@Embeddable
	public static class CompositeName implements Serializable {
		private String value1;
		private String value2;

		public CompositeName() {

		}

		public CompositeName(String value1, String value2) {
			this.value1 = value1;
			this.value2 = value2;
		}

		public String getValue1() {
			return value1;
		}

		public void setValue1(String value1) {
			this.value1 = value1;
		}

		public String getValue2() {
			return value2;
		}

		public void setValue2(String value2) {
			this.value2 = value2;
		}
	}

	private Integer ruleName1Id;
	private Integer ruleName2Id;
	private Integer alertId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			RuleName ruleName1 = new RuleName();
			RuleName ruleName2 = new RuleName();

			CompositeName compositeName1 = new CompositeName( "First1", "Last1" );
			CompositeName compositeName2 = new CompositeName( "First2", "Last2" );

			Alert alert = new Alert();
			alert.getRuleNames().add( ruleName1 );
			alert.getRuleNames().add( ruleName2 );
			alert.getNames().add( "N1" );
			alert.getNames().add( "N2" );
			alert.getComposites().add( compositeName1 );
			alert.getComposites().add( compositeName2 );

			// Revision 1
			em.getTransaction().begin();
			em.persist( ruleName1 );
			em.persist( ruleName2 );
			em.persist( alert );
			em.getTransaction().commit();

			alertId = alert.id;
			ruleName1Id = ruleName1.id;
			ruleName2Id = ruleName2.id;
		} );
	}

	@Test
	@Order(1)
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( Alert.class, alertId ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( RuleName.class, ruleName1Id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( RuleName.class, ruleName2Id ) );
		} );
	}

	@Test
	@Order(2)
	public void testClearAndAddWithinTransactionDoesNotChangeAnything(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final Alert alert = em.find( Alert.class, alertId );

			List<RuleName> ruleNamesClone = new ArrayList<>( alert.getRuleNames() );
			List<String> namesClone = new ArrayList<>( alert.getNames() );
			List<CompositeName> compositeNamesClones = new ArrayList<>( alert.getComposites() );

			alert.getRuleNames().clear();
			alert.getRuleNames().addAll( ruleNamesClone );

			alert.getNames().clear();
			alert.getNames().addAll( namesClone );

			alert.getComposites().clear();
			alert.getComposites().addAll( compositeNamesClones );

			em.persist( alert );
		} );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( Alert.class, alertId ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( RuleName.class, ruleName1Id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( RuleName.class, ruleName2Id ) );
		} );
	}

	@Test
	@Order(3)
	public void testClearAddDetachedOutsideTransaction(EntityManagerFactoryScope scope) {
		final RuleName ruleName1;
		final RuleName ruleName2;
		final CompositeName compositeName1 = new CompositeName( "First1", "Last1" );
		final CompositeName compositeName2 = new CompositeName( "First2", "Last2" );

		// Load entities outside transaction
		ruleName1 = scope.fromTransaction( em -> em.find( RuleName.class, ruleName1Id ) );
		ruleName2 = scope.fromTransaction( em -> em.find( RuleName.class, ruleName2Id ) );

		List<RuleName> ruleNamesClone = Arrays.asList( ruleName1, ruleName2 );
		List<String> namesClone = Arrays.asList( "N1", "N2" );
		List<CompositeName> compositeNamesClone = Arrays.asList( compositeName1, compositeName2 );

		scope.inTransaction( em -> {
			Alert alert = em.find( Alert.class, alertId );

			alert.getRuleNames().clear();
			alert.getRuleNames().addAll( ruleNamesClone );

			alert.getNames().clear();
			alert.getNames().addAll( namesClone );

			alert.getComposites().clear();
			alert.getComposites().addAll( compositeNamesClone );

			em.persist( alert );
		} );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( Alert.class, alertId ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( RuleName.class, ruleName1Id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( RuleName.class, ruleName2Id ) );
		} );
	}

	@Test
	@Order(4)
	public void testClearAddOneWithinTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Alert alert = em.find( Alert.class, alertId );

			List<RuleName> ruleNamesClone = new ArrayList<>( alert.getRuleNames() );
			List<String> namesClone = new ArrayList<>( alert.getNames() );
			List<CompositeName> compositeNamesClones = new ArrayList<>( alert.getComposites() );

			alert.getRuleNames().clear();
			alert.getRuleNames().add( ruleNamesClone.get( 0 ) );

			alert.getNames().clear();
			alert.getNames().add( namesClone.get( 0 ) );

			alert.getComposites().clear();
			alert.getComposites().add( compositeNamesClones.get( 0 ) );

			em.persist( alert );
		} );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( Alert.class, alertId ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( RuleName.class, ruleName1Id ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( RuleName.class, ruleName2Id ) );
		} );
	}
}
