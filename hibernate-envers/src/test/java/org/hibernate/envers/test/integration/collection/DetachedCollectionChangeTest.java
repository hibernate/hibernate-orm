/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-13080")
public class DetachedCollectionChangeTest extends BaseEnversJPAFunctionalTestCase {
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Alert.class, RuleName.class };
	}

	private Integer ruleName1Id;
	private Integer ruleName2Id;
	private Integer alertId;

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

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
	}

	@Test
	@Priority(9)
	public void testRevisionsCounts() {
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Alert.class, alertId ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( RuleName.class, ruleName1Id ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( RuleName.class, ruleName2Id ) );
	}

	@Test
	@Priority(8)
	public void testClearAndAddWithinTransactionDoesNotChangeAnything() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

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
		em.getTransaction().commit();

		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Alert.class, alertId ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( RuleName.class, ruleName1Id ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( RuleName.class, ruleName2Id ) );
	}

	@Test
	@Priority(7)
	public void testClearAddDetachedOutsideTransaction() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		final RuleName ruleName1 = em.find( RuleName.class, ruleName1Id );
		final RuleName ruleName2 = em.find( RuleName.class, ruleName2Id );

		final CompositeName compositeName1 = new CompositeName( "First1", "Last1" );
		final CompositeName compositeName2 = new CompositeName( "First2", "Last2" );

		List<RuleName> ruleNamesClone = Arrays.asList( ruleName1, ruleName2 );
		List<String> namesClone = Arrays.asList( "N1", "N2" );
		List<CompositeName> compositeNamesClone = Arrays.asList( compositeName1, compositeName2 );

		em.getTransaction().rollback();

		em.getTransaction().begin();
		Alert alert = em.find( Alert.class, alertId );

		alert.getRuleNames().clear();
		alert.getRuleNames().addAll( ruleNamesClone );

		alert.getNames().clear();
		alert.getNames().addAll( namesClone );

		alert.getComposites().clear();
		alert.getComposites().addAll( compositeNamesClone );

		em.persist( alert );
		em.getTransaction().commit();

		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Alert.class, alertId ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( RuleName.class, ruleName1Id ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( RuleName.class, ruleName2Id ) );
	}

	@Test
	@Priority(6)
	public void testClearAddOneWithinTransaction() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

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
		em.getTransaction().commit();

		Assert.assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( Alert.class, alertId ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( RuleName.class, ruleName1Id ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( RuleName.class, ruleName2Id ) );
	}
}
