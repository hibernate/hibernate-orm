package org.hibernate.orm.test.sql.results.graph.embeddable.internal;

import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = {
				EmbeddableFetchImplTests.ExpressCompany.class,
				EmbeddableFetchImplTests.Address.class,
				EmbeddableFetchImplTests.Country.class
		}
)
@SessionFactory
public class EmbeddableFetchImplTests {

	@Test
	void testTableGroupJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				// will throw exception if the patch accompanying this test is not applied
				// for 'country' alias shows up in 'select', but not in 'from' clause
				session -> session.get( ExpressCompany.class, "nonexistent" )
		);
	}

	@Entity(name = "ExpressCompany")
	public static class ExpressCompany {
		@Id
		String name;

		@ElementCollection(fetch = FetchType.EAGER)
		Set<Address> shipAddresses;
	}

	@Embeddable
	public static class Address {
		@ManyToOne
		@Fetch(FetchMode.JOIN)
		Country country;
	}

	@Entity(name = "Country")
	public static class Country {
		@Id
		String name;
	}

}
