/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.ordering;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.mapping.ordering.ast.OrderByComplianceViolation;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class OrderByComplianceTests {
	@Test
	@ServiceRegistry( settings = @Setting( name= AvailableSettings.JPA_ORDER_BY_MAPPING_COMPLIANCE, value = "false" ) )
	@DomainModel( annotatedClasses = { TypesOfThings.class, Contact.class, AddressBook.class } )
	public void testNonCompliantBaseline(DomainModelScope scope) {
		final SessionFactory sessionFactory = scope.getDomainModel().buildSessionFactory();
		assertThat( sessionFactory ).isNotNull();
	}

	@Test
	@ServiceRegistry( settings = @Setting( name= AvailableSettings.JPA_ORDER_BY_MAPPING_COMPLIANCE, value = "true" ) )
	@DomainModel( annotatedClasses = { TypesOfThings.class, Contact.class, AddressBook.class } )
	public void testNonCompliantStrictly(DomainModelScope scope) {
		try {
			final SessionFactory sessionFactory = scope.getDomainModel().buildSessionFactory();
			assertThat( sessionFactory ).isNotNull();
			fail( "Expecting a failure here" );
		}
		catch (OrderByComplianceViolation exception) {
			assertThat( exception.getMessage() ).isEqualTo(
					"@OrderBy expression (last_name) is not a domain-model reference, " +
							"which violates the Jakarta Persistence specification - column reference (last_name)"
			);
		}
	}

	@Test
	@ServiceRegistry( settings = @Setting( name= AvailableSettings.JPA_ORDER_BY_MAPPING_COMPLIANCE, value = "false" ) )
	@DomainModel( annotatedClasses = { TypesOfThings.class, Contact.class, CompliantAddressBook.class } )
	public void testCompliantBaseline(DomainModelScope scope) {
		final SessionFactory sessionFactory = scope.getDomainModel().buildSessionFactory();
		assertThat( sessionFactory ).isNotNull();
	}

	@Test
	@ServiceRegistry( settings = @Setting( name= AvailableSettings.JPA_ORDER_BY_MAPPING_COMPLIANCE, value = "true" ) )
	@DomainModel( annotatedClasses = { TypesOfThings.class, Contact.class, CompliantAddressBook.class } )
	public void testCompliantStrictly(DomainModelScope scope) {
		final SessionFactory sessionFactory = scope.getDomainModel().buildSessionFactory();
		assertThat( sessionFactory ).isNotNull();
	}

}
