/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.mappedsuper;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class ImpliedMappedSuperTests {
	@Test
	@DomainModel( xmlMappings = "mappings/models/hbm/mappedsuper/implied-mapped-super.xml" )
	void testHbm(DomainModelScope domainModelScope) {
		verify( domainModelScope.getDomainModel() );
	}

	@Test
	@FailureExpected( reason = "Support for implied mapped-superclass from hbm.xml is not implemented yet" )
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18387" )
	@ServiceRegistry( settings = @Setting( name = MappingSettings.TRANSFORM_HBM_XML, value = "true" ) )
	void testHbmTransformation(ServiceRegistryScope registryScope) {
		final Metadata domainModel = new MetadataSources( registryScope.getRegistry() )
				.addResource( "mappings/models/hbm/mappedsuper/implied-mapped-super.xml" )
				.buildMetadata();
		verify( domainModel );
	}

	private void verify(Metadata domainModel) {
		final PersistentClass thing1Binding = domainModel.getEntityBinding( Thing1.class.getName() );
		final PersistentClass thing2Binding = domainModel.getEntityBinding( Thing2.class.getName() );

	}
}
