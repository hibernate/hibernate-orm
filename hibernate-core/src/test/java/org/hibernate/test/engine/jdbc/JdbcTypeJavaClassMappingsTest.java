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
package org.hibernate.test.engine.jdbc;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Timestamp;
import java.sql.Types;

import org.hibernate.type.descriptor.sql.JdbcTypeJavaClassMappings;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertJdbcTypeCode;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class JdbcTypeJavaClassMappingsTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-7795" )
	public void testBaselineMappings() {
		final JdbcTypeJavaClassMappings mappings = new JdbcTypeJavaClassMappings();

		assertJdbcTypeCode( Types.VARCHAR, mappings.determineJdbcTypeCodeForJavaClass( String.class ) );
		assertJdbcTypeCode( Types.CHAR, mappings.determineJdbcTypeCodeForJavaClass( Character.class ) );
		assertJdbcTypeCode( Types.INTEGER, mappings.determineJdbcTypeCodeForJavaClass( Integer.class ) );
		assertJdbcTypeCode( Types.CLOB, mappings.determineJdbcTypeCodeForJavaClass( Clob.class ) );
		assertJdbcTypeCode( Types.NCLOB, mappings.determineJdbcTypeCodeForJavaClass( NClob.class ) );
		assertJdbcTypeCode( Types.BLOB, mappings.determineJdbcTypeCodeForJavaClass( Blob.class ) );

		assertEquals( String.class, mappings.determineJavaClassForJdbcTypeCode( Types.VARCHAR ) );
		assertEquals( String.class, mappings.determineJavaClassForJdbcTypeCode( Types.CHAR ) );
		assertEquals( String.class, mappings.determineJavaClassForJdbcTypeCode( Types.NVARCHAR ) );
		assertEquals( String.class, mappings.determineJavaClassForJdbcTypeCode( Types.NCHAR ) );
		assertEquals( Integer.class, mappings.determineJavaClassForJdbcTypeCode( Types.INTEGER ) );
		assertEquals( Timestamp.class, mappings.determineJavaClassForJdbcTypeCode( Types.TIMESTAMP ) );
	}
}
