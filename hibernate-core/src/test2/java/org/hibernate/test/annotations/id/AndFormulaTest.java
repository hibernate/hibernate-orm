/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.id;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Formula;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.CannotForceNonNullableException;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Originally developed for HHH-9807 - better error message on combination of {@code @Id} + {@code @Formula}
 *
 * @author Steve Ebersole
 */
public class AndFormulaTest extends BaseUnitTestCase {
	private static StandardServiceRegistry ssr;

	@BeforeClass
	public static void prepareServiceRegistry() {
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@AfterClass
	public static void releaseServiceRegistry() {
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
			assertThat( e.getMessage(), startsWith( "Identifier property [" ) );
		}
	}

	@Entity
	public static class EntityWithIdAndFormula {
		@Id
		@Formula( value = "VALUE" )
		public Integer id;
	}
}
