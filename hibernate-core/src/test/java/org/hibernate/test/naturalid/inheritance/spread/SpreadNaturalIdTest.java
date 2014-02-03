/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.naturalid.inheritance.spread;

import static org.junit.Assert.fail;

import org.hibernate.AnnotationException;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7129" )
@FailureExpectedWithNewMetamodel
public class SpreadNaturalIdTest extends BaseUnitTestCase {
	@Test
	public void testSpreadNaturalIdDeclarationGivesMappingException() {
		Configuration cfg = new Configuration()
				.addAnnotatedClass( Principal.class )
				.addAnnotatedClass( User.class );
		try {
			cfg.buildMappings();
			fail( "Expected binders to throw an exception" );
		}
		catch (AnnotationException expected) {
		}
	}
}
