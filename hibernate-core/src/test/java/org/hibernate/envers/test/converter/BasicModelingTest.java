/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.converter;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.converter.Person;
import org.hibernate.envers.test.support.domains.converter.SexConverter;
import org.hibernate.mapping.PersistentClass;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Steve Ebersole
 */
@Disabled("Attempts to instantiate Sex enum via ManagedBeanRegistry throwing unable to locate no-arg constructor for bean.")
public class BasicModelingTest extends EnversSessionFactoryBasedFunctionalTest {
	@DynamicTest
	@TestForIssue( jiraKey = "HHH-9042" )
	public void testMetamodelBuilding() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();
		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Person.class )
					.getMetadataBuilder()
					.applyAttributeConverter( SexConverter.class )
					.build();

			( (MetadataImpl) metadata ).validate();

			PersistentClass personBinding = metadata.getEntityBinding( Person.class.getName() );
			assertThat( personBinding, notNullValue() );

			PersistentClass personAuditBinding = metadata.getEntityBinding( Person.class.getName() + "_AUD" );
			assertThat( personAuditBinding, notNullValue() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
