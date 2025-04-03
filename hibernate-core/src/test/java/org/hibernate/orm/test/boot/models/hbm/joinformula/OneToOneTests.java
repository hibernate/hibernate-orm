/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.orm.test.onetoone.formula.Person;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests parsing of {@code <join-formula/>} for a one-to-one
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class OneToOneTests {
	@Test
	@DomainModel(xmlMappings = "mappings/models/joinformula/one-to-one.xml")
	void testMappingXml(DomainModelScope domainModelScope) {
		final PersistentClass personEntityBinding = domainModelScope.getDomainModel().getEntityBinding( Person.class.getName() );
		validateAddress( personEntityBinding.getProperty( "address" ) );
		validateMailingAddress( personEntityBinding.getProperty( "mailingAddress" ) );
	}

	private void validateAddress(Property property) {
		final ToOne valueMapping = (ToOne) property.getValue();
		assertThat( valueMapping.getSelectables() ).hasSize( 2 );
		assertThat( valueMapping.getSelectables().get(0) ).isInstanceOf( Formula.class );
		assertThat( valueMapping.getSelectables().get(1) ).isInstanceOf( Formula.class );
	}

	private void validateMailingAddress(Property property) {
		final ToOne valueMapping = (ToOne) property.getValue();
		assertThat( valueMapping.getSelectables() ).hasSize( 2 );
		assertThat( valueMapping.getSelectables().get(0) ).isInstanceOf( Formula.class );
		assertThat( valueMapping.getSelectables().get(1) ).isInstanceOf( Formula.class );
	}
}
