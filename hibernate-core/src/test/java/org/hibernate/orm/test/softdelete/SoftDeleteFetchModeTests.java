/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
public class SoftDeleteFetchModeTests {
	@Test
	@JiraKey( "HHH-19920" )
	void checkMappings(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Root.class, Child.class )
				.buildMetadata();
		try (SessionFactoryImplementor sessionFactory = metadata.buildSessionFactory()) {
			fail( "Expecting UnsupportedMappingException" );
		}
		catch (UnsupportedMappingException expected) {
			assertThat( expected ).hasMessageContainingAll(
					"Root.child",
					"cannot be mapped as LAZY as its associated entity is defined with @SoftDelete"
			);
		}
	}

	@Entity(name="Root")
	@Table(name="Root")
	@SoftDelete
	public static class Root {
		@Id
		private Integer id;
		private String name;

		// the maps-id is irrelevant for the error
		//@MapsId
		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "child_fk")
		private Child child;
	}

	@Entity(name="Child")
	@Table(name="Child")
	@SoftDelete
	public static class Child {
		@Id
		private Integer id;
		private String name;
	}
}
