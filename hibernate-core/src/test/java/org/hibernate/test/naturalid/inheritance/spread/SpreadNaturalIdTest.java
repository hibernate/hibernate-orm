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

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7129" )
public class SpreadNaturalIdTest extends BaseUnitTestCase {
	@Test
	@SuppressWarnings("EmptyCatchBlock")
	public void testSpreadNaturalIdDeclarationGivesMappingException() {
		try {
			new MetadataSources()
					.addAnnotatedClass( Principal.class )
					.addAnnotatedClass( User.class )
					.buildMetadata();
			fail( "Expected binders to throw an exception" );
		}
		catch (AnnotationException expected) {
		}
	}
}
