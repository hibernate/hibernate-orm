/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.orm.test.onetoone.formula.Person;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class OneToOneTests {
	@Test
	@DomainModel(xmlMappings = "org/hibernate/orm/test/onetoone/formula/Person.hbm.xml")
	void testHbmXml(DomainModelScope domainModelScope) {
		final PersistentClass personEntityBinding = domainModelScope.getDomainModel().getEntityBinding( Person.class.getName() );
		validateAddress( personEntityBinding.getProperty( "address" ) );
		validateMailingAddress( personEntityBinding.getProperty( "mailingAddress" ) );
	}

	@Test
	@ServiceRegistry( settings = @Setting( name= MappingSettings.TRANSFORM_HBM_XML, value = "true" ) )
	@DomainModel(xmlMappings = "org/hibernate/orm/test/onetoone/formula/Person.hbm.xml")
	void testTransformation(DomainModelScope domainModelScope) {
		final PersistentClass personEntityBinding = domainModelScope.getDomainModel().getEntityBinding( Person.class.getName() );
		validateAddress( personEntityBinding.getProperty( "address" ) );
		validateMailingAddress( personEntityBinding.getProperty( "mailingAddress" ) );
	}

	@Test
	@DomainModel(xmlMappings = "mappings/models/hbm/joinformula/one-to-one.xml")
	void testMappingXml(DomainModelScope domainModelScope) {
		final PersistentClass personEntityBinding = domainModelScope.getDomainModel().getEntityBinding( Person.class.getName() );
		validateAddress( personEntityBinding.getProperty( "address" ) );
		validateMailingAddress( personEntityBinding.getProperty( "mailingAddress" ) );
	}

	private void validateAddress(Property property) {

	}

	private void validateMailingAddress(Property property) {

	}
}
