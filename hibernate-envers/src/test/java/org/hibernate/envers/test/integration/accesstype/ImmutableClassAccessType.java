package org.hibernate.envers.test.integration.accesstype;

import java.util.Arrays;

import javax.persistence.EntityManager;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class ImmutableClassAccessType extends AbstractEntityTest {
	private Country country;

	public void configure(Ejb3Configuration cfg) {
		cfg.addAnnotatedClass(Country.class);
	}

	@BeforeClass(dependsOnMethods = "init")
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		country = Country.of(123, "Germany");
		em.persist(country);
		em.getTransaction().commit();

	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList(1)
				.equals(
						getAuditReader().getRevisions(Country.class,
								country.getCode()));
	}

	@Test
	public void testHistoryOfId1() {
		Country country1 = getEntityManager().find(Country.class,
				country.getCode());
		assertEquals(country1, country);

		Country history = getAuditReader().find(Country.class, country1.getCode(), 1);
		assertEquals(country, history);
	}

}