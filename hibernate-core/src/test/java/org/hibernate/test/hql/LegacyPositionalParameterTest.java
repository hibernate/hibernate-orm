/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.hql;

import java.util.Map;

import org.hibernate.QueryException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.internal.ParameterMetadataImpl;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class LegacyPositionalParameterTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.JDBC_STYLE_PARAMS_ZERO_BASE, true );
	}

	@Override
	protected void afterMetadataSourcesApplied(MetadataSources metadataSources) {
		super.afterMetadataSourcesApplied( metadataSources );
		metadataSources.addAnnotatedClass( Title.class );
	}

	@Test
	public void testLegacyParameterUsage() {
		inTransaction(
				session -> {
					session.createQuery( "from Title t where t.description = ?" )
							.setParameter( 0, "the title" )
							.list();
				}
		);
	}

	@Test
	public void testIncorrectLegacyParameterLabelUsage() {
		try {
			inTransaction(
					session -> {
						session.createQuery( "from Title t where t.description = ?" )
								.setParameter( 1, "the title" )
								.list();
					}
			);
			fail();
		}
		catch (IllegalArgumentException expected) {
			assertThat( expected.getMessage(), startsWith( "Could not locate ordinal parameter" ) );
		}
	}

	@Test
	public void testJpaParameterUsage() {
		inTransaction(
				session -> {
					session.createQuery( "from Title t where t.description = ?1" )
							.setParameter( 1, "the title" )
							.list();
				}
		);
	}

	@Test
	public void testJpaParameterUsageIncorrectLabel() {
		try {
			inTransaction(
					session -> {
						session.createQuery( "from Title t where t.description = ?5" )
								.setParameter( 5, "the title" )
								.list();
					}
			);
			fail();
		}
		catch (IllegalArgumentException expected) {
			assertThat( expected.getCause(), instanceOf( QueryException.class ) );
			final QueryException expectedExpected = (QueryException) expected.getCause();
			assertThat( expectedExpected.getMessage(), startsWith( "Unexpected ordinal parameter label base" ) );
		}
	}

	@Test
	public void testJpaParameterUsageIncorrectBase() {
		try {
			inTransaction(
					session -> {
						session.createQuery( "from Title t where t.description = ?0" )
								.setParameter( 0, "the title" )
								.list();
					}
			);
			fail();
		}
		catch (IllegalArgumentException expected) {
			assertThat( expected.getCause(), instanceOf( QueryException.class ) );
			final QueryException expectedExpected = (QueryException) expected.getCause();
			assertThat( expectedExpected.getMessage(), startsWith( "Unexpected ordinal parameter label base" ) );
		}
	}

	@Test
	public void testMixedParameterUsage() {
		try {
			inTransaction(
					session -> {
						session.createQuery( "from Title t where t.description = ?1 or t.description = ?" )
								.setParameter( 0, "the other title" )
								.setParameter( 1, "the title" )
								.list();
					}
			);
			fail( "Expecting failure" );
		}
		catch (IllegalArgumentException expected) {
			assertThat( expected.getCause(), instanceOf( QueryException.class ) );
			final QueryException expectedExpected = (QueryException) expected.getCause();
			assertThat( expectedExpected.getMessage(), CoreMatchers.startsWith( ParameterMetadataImpl.MIXED_POSITIONAL_PARAM_STYLE_ERROR_MSG ) );
		}
	}
}
