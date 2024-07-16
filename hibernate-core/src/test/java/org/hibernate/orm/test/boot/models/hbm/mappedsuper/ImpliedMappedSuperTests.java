/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.mappedsuper;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
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
		verify( domainModelScope );
	}

	@Test
	@FailureExpected( reason = "Support for implied mapped-superclass from hbm.xml is not implemented yet" )
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18387" )
	@ServiceRegistry( settings = @Setting( name = MappingSettings.TRANSFORM_HBM_XML, value = "true" ) )
	@DomainModel( xmlMappings = "mappings/models/hbm/mappedsuper/implied-mapped-super.xml" )
	void testHbmTransformation(DomainModelScope domainModelScope) {
		verify( domainModelScope );
	}

	private void verify(DomainModelScope domainModelScope) {
		final PersistentClass thing1Binding = domainModelScope.getEntityBinding( Thing1.class );
		final PersistentClass thing2Binding = domainModelScope.getEntityBinding( Thing2.class );

	}
}
