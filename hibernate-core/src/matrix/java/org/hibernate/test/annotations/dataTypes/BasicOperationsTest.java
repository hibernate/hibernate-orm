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
package org.hibernate.test.annotations.dataTypes;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature( DialectChecks.SupportsExpectedLobUsagePattern.class )
public class BasicOperationsTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SomeEntity.class, SomeOtherEntity.class };
	}

	@Test
	public void testCreateAndDelete() {
		Date now = new Date();

		Session s = openSession();
		s.doWork(
				new Work() {
					public void execute(Connection connection) throws SQLException {
						// id -> java.util.Date (DATE - becase of explicit TemporalType)
						validateColumn( connection, "ID", java.sql.Types.DATE );

						// timeData -> java.sql.Time (TIME)
						validateColumn( connection, "TIMEDATA", java.sql.Types.TIME );

						// tsData -> java.sql.Timestamp (TIMESTAMP)
						validateColumn( connection, "TSDATA", java.sql.Types.TIMESTAMP );
					}

					private void validateColumn(Connection connection, String columnName, int expectedJdbcTypeCode)
							throws SQLException {
						ResultSet columnInfo = connection.getMetaData().getColumns( null, null, "SOMEENTITY", columnName );
						assertTrue( columnInfo.next() );
						int dataType = columnInfo.getInt( "DATA_TYPE" );
						columnInfo.close();
						assertEquals( columnName, JdbcTypeNameMapper.getTypeName(expectedJdbcTypeCode), JdbcTypeNameMapper.getTypeName(dataType) );
					}

				}
		);
		s.beginTransaction();
		SomeEntity someEntity = new SomeEntity( now );
		SomeOtherEntity someOtherEntity = new SomeOtherEntity(1);
		s.save( someEntity );
		s.save( someOtherEntity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( someEntity );
		s.delete( someOtherEntity );
		s.getTransaction().commit();
		s.close();
	}
}
