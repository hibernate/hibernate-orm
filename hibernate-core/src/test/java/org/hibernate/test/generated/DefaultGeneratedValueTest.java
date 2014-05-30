/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.generated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.tuple.ValueGenerator;
import org.junit.Test;

/**
 * Test for the generation of column values using different
 * {@link org.hibernate.tuple.ValueGeneration} implementations.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 */
@FailureExpectedWithNewMetamodel
@SkipForDialect(value=SybaseDialect.class, comment="CURRENT_TIMESTAMP not supported as default value in Sybase")
public class DefaultGeneratedValueTest extends BaseCoreFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-2907" )
	public void testGeneration() {
		Session s = openSession();
		s.beginTransaction();
		TheEntity theEntity = new TheEntity( 1 );
		assertNull( theEntity.createdDate );
		assertNull( theEntity.alwaysDate );
		assertNull( theEntity.vmCreatedDate );
		assertNull( theEntity.vmCreatedSqlDate );
		assertNull( theEntity.vmCreatedSqlTime );
		assertNull( theEntity.vmCreatedSqlTimestamp );
		assertNull( theEntity.name );
		s.save( theEntity );
		//TODO: Actually the values should be non-null after save
		assertNull( theEntity.createdDate );
		assertNull( theEntity.alwaysDate );
		assertNull( theEntity.vmCreatedDate );
		assertNull( theEntity.vmCreatedSqlDate );
		assertNull( theEntity.vmCreatedSqlTime );
		assertNull( theEntity.vmCreatedSqlTimestamp );
		assertNull( theEntity.name );
		s.getTransaction().commit();
		s.close();

		assertNotNull( theEntity.createdDate );
		assertNotNull( theEntity.alwaysDate );
		assertEquals( "Bob", theEntity.name );

		s = openSession();
		s.beginTransaction();
		theEntity = (TheEntity) s.get( TheEntity.class, 1 );
		assertNotNull( theEntity.createdDate );
		assertNotNull( theEntity.alwaysDate );
		assertNotNull( theEntity.vmCreatedDate );
		assertNotNull( theEntity.vmCreatedSqlDate );
		assertNotNull( theEntity.vmCreatedSqlTime );
		assertNotNull( theEntity.vmCreatedSqlTimestamp );
		assertEquals( "Bob", theEntity.name );

		theEntity.lastName = "Smith";
		s.delete( theEntity );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-2907")
	public void testUpdateTimestampGeneration() {
		Session s = openSession();
		s.beginTransaction();
		TheEntity theEntity = new TheEntity( 1 );
		assertNull( theEntity.updated );
		s.save( theEntity );

		//TODO: Actually the value should be non-null after save
		assertNull( theEntity.updated );
		s.getTransaction().commit();
		s.close();

		Timestamp created = theEntity.vmCreatedSqlTimestamp;
		Timestamp updated = theEntity.updated;
		assertNotNull( updated );
		assertNotNull( created );

		s = openSession();
		s.beginTransaction();

		theEntity = (TheEntity) s.get( TheEntity.class, 1 );
		theEntity.lastName = "Smith";

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		theEntity = (TheEntity) s.get( TheEntity.class, 1 );

		assertEquals( "Creation timestamp should not change on update", created, theEntity.vmCreatedSqlTimestamp );
		assertTrue( "Update timestamp should have changed due to update", theEntity.updated.after( updated ) );

		s.delete( theEntity );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TheEntity.class };
	}

	@Entity( name = "TheEntity" )
	@Table( name = "T_ENT_GEN_DEF" )
	private static class TheEntity {
		@Id
		private Integer id;

		@Generated( GenerationTime.INSERT )
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		@Column( nullable = false )
		private Date createdDate;

		@Generated( GenerationTime.ALWAYS )
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		@Column( nullable = false )
		private Calendar alwaysDate;

		@CreationTimestamp
		private Date vmCreatedDate;

		@CreationTimestamp
		private Calendar vmCreatedCalendar;

		@CreationTimestamp
		private java.sql.Date vmCreatedSqlDate;

		@CreationTimestamp
		private Time vmCreatedSqlTime;

		@CreationTimestamp
		private Timestamp vmCreatedSqlTimestamp;

		@UpdateTimestamp
		private Timestamp updated;

		@GeneratorType( type = MyVmValueGenerator.class, when = GenerationTime.INSERT )
		private String name;

		@SuppressWarnings("unused")
		private String lastName;

		private TheEntity() {
		}

		private TheEntity(Integer id) {
			this.id = id;
		}
	}

	public static class MyVmValueGenerator implements ValueGenerator<String> {

		@Override
		public String generateValue(Session session, Object owner) {
			return "Bob";
		}
	}
}
