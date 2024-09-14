/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.orm.test.unconstrained.Person;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class ManyToOneTests {
	@Test
	@DomainModel(xmlMappings = "org/hibernate/orm/test/unconstrained/Person.hbm.xml")
	void testHbmXml(DomainModelScope domainModelScope) {
		final PersistentClass personEntityBinding = domainModelScope.getDomainModel().getEntityBinding( Person.class.getName() );
		validate( personEntityBinding.getProperty( "employee" ) );
	}

	@Test
	@ServiceRegistry( settings = @Setting( name= MappingSettings.TRANSFORM_HBM_XML, value = "true" ) )
	@DomainModel(xmlMappings = "org/hibernate/orm/test/unconstrained/Person.hbm.xml")
	void testTransformation(DomainModelScope domainModelScope) {
		final PersistentClass personEntityBinding = domainModelScope.getDomainModel().getEntityBinding( Person.class.getName() );
		validate( personEntityBinding.getProperty( "employee" ) );
	}

	@Test
	@DomainModel(xmlMappings = "mappings/models/hbm/joinformula/many-to-one.xml")
	void testMappingXml(DomainModelScope domainModelScope) {
		final PersistentClass personEntityBinding = domainModelScope.getDomainModel().getEntityBinding( Person.class.getName() );
		validate( personEntityBinding.getProperty( "employee" ) );
	}

	private void validate(Property property) {
		final ManyToOne manyToOne = (ManyToOne) property.getValue();
		assertThat( manyToOne.getSelectables() ).hasSize( 1 );
		assertThat( manyToOne.getSelectables().get(0) ).isInstanceOf( Formula.class );

		assertThat( property.getCascade() ).isEqualTo( "all" );
	}
}
