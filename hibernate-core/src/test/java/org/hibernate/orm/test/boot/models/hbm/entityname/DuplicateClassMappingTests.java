/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.entityname;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class DuplicateClassMappingTests {
	@Test
	@DomainModel(xmlMappings = "mappings/models/hbm/entityname/hbm.xml")
	void testHbmXml(DomainModelScope domainModelScope) {
		assertThat( domainModelScope.getDomainModel().getEntityBinding( "BillingAddress" ) ).isNotNull();
		assertThat( domainModelScope.getDomainModel().getEntityBinding( "ShippingAddress" ) ).isNotNull();
	}

	@Test
	@ServiceRegistry( settings = @Setting( name= MappingSettings.TRANSFORM_HBM_XML, value = "true" ) )
	void testHbmXmlTransformation(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() );
		metadataSources.addResource( "mappings/models/hbm/entityname/hbm.xml" );
		try {
			metadataSources.buildMetadata();
			fail( "Expecting a failure" );
		}
		catch (MappingException expected) {
			assertThat( expected.getMessage() ).contains( Address.class.getName() );
			assertThat( expected.getMessage() ).contains( "ShippingAddress" );
			assertThat( expected.getMessage() ).contains( "BillingAddress" );
		}
	}
}
