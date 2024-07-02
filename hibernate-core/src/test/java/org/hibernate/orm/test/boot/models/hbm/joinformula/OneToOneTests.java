/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.orm.test.onetoone.formula.Person;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
	void testTransformation(ServiceRegistryScope registryScope) {
		try {
			final StandardServiceRegistry serviceRegistry = registryScope.getRegistry();
			final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
			metadataSources.addResource( "org/hibernate/orm/test/onetoone/formula/Person.hbm.xml" );
			final Metadata domainModel = metadataSources.buildMetadata();

			final PersistentClass personEntityBinding = domainModel.getEntityBinding( Person.class.getName() );
			validateAddress( personEntityBinding.getProperty( "address" ) );
			validateMailingAddress( personEntityBinding.getProperty( "mailingAddress" ) );
		}
		catch (UnsupportedOperationException e) {
			assertThat( e.getMessage() ).contains( "<key-many-to-one/>" );
		}
	}

	@Test
	@DomainModel(xmlMappings = "mappings/models/hbm/joinformula/one-to-one.xml")
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
