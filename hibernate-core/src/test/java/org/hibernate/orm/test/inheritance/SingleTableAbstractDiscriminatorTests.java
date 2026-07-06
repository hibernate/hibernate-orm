/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

/**
 * Tests that abstract classes within the hierarchy do not need {@linkplain DiscriminatorValue}
 *
 * @author Steve Ebersole
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-5798" )
@ServiceRegistry
public class SingleTableAbstractDiscriminatorTests {
	@Test
	void testCompliantModel(ServiceRegistryScope registryScope) {
		var model = MetadataBuildingTestHelper.buildMetadata(
				registryScope.getRegistry(),
				new MappingSources()
						.addManagedClass( Top.class )
						.addManagedClass( Middle.class )
						.addManagedClass( Bottom.class )
		);
		try (var sf = (SessionFactoryImplementor) org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( model )) {
		}
	}

	@Test
	void testNonCompliantModel(ServiceRegistryScope registryScope) {
		// technically, according to JPA, this one should fail -
		//
		// 		> The DiscriminatorValue annotation can only be specified on a concrete entity class.
		//
		// we do not validate this though
		var model = MetadataBuildingTestHelper.buildMetadata(
				registryScope.getRegistry(),
				new MappingSources()
						.addManagedClass( Root.class )
						.addManagedClass( Trunk.class )
						.addManagedClass( Branch.class )
		);
		try (var sf = (SessionFactoryImplementor) org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( model )) {
		}
	}

	@Entity
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn
	public static abstract class Top {
		@Id
		private Integer id;
		private String name;
	}

	@Entity
	public static abstract class Middle extends Top {
	}

	@Entity
	@DiscriminatorValue( "B" )
	public static class Bottom extends Middle {
	}

	@Entity
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn
	public static abstract class Root {
		@Id
		private Integer id;
		private String name;
	}

	@Entity
	@DiscriminatorValue( "illegal" )
	public static abstract class Trunk extends Root {
	}

	@Entity
	@DiscriminatorValue( "b" )
	public static class Branch extends Trunk {
		@Id
		private Integer id;
		private String name;
	}
}
