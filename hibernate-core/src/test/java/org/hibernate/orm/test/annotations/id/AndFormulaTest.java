/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id;

import org.hibernate.annotations.Formula;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.internal.CannotForceNonNullableException;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Originally developed for HHH-9807 - better error message on combination of {@code @Id} + {@code @Formula}
 *
 * @author Steve Ebersole
 */
@ServiceRegistry
@BaseUnitTest
public class AndFormulaTest {
	private static StandardServiceRegistry ssr;

	@BeforeEach
	public void prepareServiceRegistry() {
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterEach
	public void releaseServiceRegistry() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testBindingEntityWithIdAndFormula() {
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( EntityWithIdAndFormula.class )
					.buildMetadata();
			fail( "Expecting failure from invalid mapping" );
		}
		catch (CannotForceNonNullableException e) {
			assertThat( e.getMessage(), startsWith( "Identifier property " ) );
		}
	}

	@Entity
	public static class EntityWithIdAndFormula {
		@Id
		@Formula( value = "VALUE" )
		public Integer id;
	}
}
