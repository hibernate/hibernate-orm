/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.orm.test.dialect.function;

import java.util.List;

import org.hibernate.dialect.function.json.JsonPathHelper;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonPathHelperTest {

	@Test
	public void testRoot() {
		assertEquals(
				List.of(),
				JsonPathHelper.parseJsonPathElements( "$" )
		);
	}

	@Test
	public void testRootArray() {
		assertEquals(
				List.of( new JsonPathHelper.JsonIndexAccess( 0 ) ),
				JsonPathHelper.parseJsonPathElements( "$[0]" )
		);
	}

	@Test
	public void testDeReferenceRootArray() {
		assertEquals(
				List.of( new JsonPathHelper.JsonIndexAccess( 0 ), new JsonPathHelper.JsonAttribute( "attribute" ) ),
				JsonPathHelper.parseJsonPathElements( "$[0].attribute" )
		);
	}

	@Test
	public void testSimplePath() {
		assertEquals(
				List.of( new JsonPathHelper.JsonAttribute( "attribute" ) ),
				JsonPathHelper.parseJsonPathElements( "$.attribute" )
		);
	}

	@Test
	public void testArrayPath() {
		assertEquals(
				List.of( new JsonPathHelper.JsonAttribute( "attribute" ), new JsonPathHelper.JsonIndexAccess( 0 ) ),
				JsonPathHelper.parseJsonPathElements( "$.attribute[0]" )
		);
	}

	@Test
	public void testDeepArrayPath() {
		assertEquals(
				List.of(
						new JsonPathHelper.JsonAttribute( "attribute" ),
						new JsonPathHelper.JsonIndexAccess( 0 ),
						new JsonPathHelper.JsonAttribute( "subAttribute" )
				),
				JsonPathHelper.parseJsonPathElements( "$.attribute[0].subAttribute" )
		);
	}
}
