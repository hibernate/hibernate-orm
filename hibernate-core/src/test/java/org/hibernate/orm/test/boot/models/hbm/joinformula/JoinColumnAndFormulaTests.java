/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class JoinColumnAndFormulaTests {
	@Test
	@DomainModel( xmlMappings = "mappings/models/hbm/joinformula/many-to-one-join-column-and-formula.xml" )
	void testHbmXml(DomainModelScope domainModelScope) {
		verifyMapping( domainModelScope );
	}

	@Test
	@DomainModel( annotatedClasses = {Address.class, Contact.class} )
	void testAnnotations(DomainModelScope domainModelScope) {
		verifyMapping( domainModelScope );
	}

	@Test
	@ServiceRegistry( settings = @Setting( name= MappingSettings.TRANSFORM_HBM_XML, value = "true" ) )
	@DomainModel( xmlMappings = "mappings/models/hbm/joinformula/many-to-one-join-column-and-formula.xml" )
	void testHbmXmlTransformed(DomainModelScope domainModelScope) {
		verifyMapping( domainModelScope );
	}

	void verifyMapping(DomainModelScope domainModelScope) {
		final PersistentClass entityBinding = domainModelScope.getEntityBinding( Contact.class );

		final Property mailingAddress = entityBinding.getProperty( "mailingAddress" );
		final Value mailingAddressValue = mailingAddress.getValue();
		assertThat( mailingAddressValue.getSelectables() ).hasSize( 2 );
		assertThat( ( (Column) mailingAddressValue.getSelectables().get( 0 ) ).getName() ).isEqualTo( "mailing_address_fk" );
		assertThat( ( (Formula) mailingAddressValue.getSelectables().get( 1 ) ).getFormula() ).isEqualTo( "'MAILING'" );

		final Property shippingAddress = entityBinding.getProperty( "shippingAddress" );
		final Value shippingAddressValue = shippingAddress.getValue();
		assertThat( shippingAddressValue.getSelectables() ).hasSize( 2 );
		assertThat( ( (Column) shippingAddressValue.getSelectables().get( 0 ) ).getName() ).isEqualTo( "shipping_address_fk" );
		assertThat( ( (Formula) shippingAddressValue.getSelectables().get( 1 ) ).getFormula() ).isEqualTo( "'SHIPPING'" );
	}
}
