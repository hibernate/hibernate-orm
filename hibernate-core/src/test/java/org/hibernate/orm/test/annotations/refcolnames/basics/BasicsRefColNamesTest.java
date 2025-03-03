/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.basics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@DomainModel(annotatedClasses = {Region.class, Town.class})
@SessionFactory
public class BasicsRefColNamesTest {
	@Test
	public void test(SessionFactoryScope scope) {
		Region region = new Region();
		PostalCode postalCode = new PostalCode();
		postalCode.countryCode = "ES";
		postalCode.zipCode = 69;
		region.countryCode = postalCode.countryCode;
		region.zipCode = postalCode.zipCode;
		Town town = new Town();
		TownCode townCode = new TownCode();
		townCode.town = "Barcelona";
		townCode.countryCode = "ES";
		townCode.zipCode = 69;
		town.region = region;
		town.townCode = townCode;
		scope.inTransaction(s -> {
			s.persist(region);
			s.persist(town);
		});
		scope.inTransaction(s -> {
			Town t = s.createQuery("from Town join fetch region", Town.class).getSingleResult();
			Assertions.assertNotNull(t);
		});
	}
}
