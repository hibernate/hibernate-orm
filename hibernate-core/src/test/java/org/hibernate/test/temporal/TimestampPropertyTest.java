/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.test.temporal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Tests that persisted timestamp properties have the expected format to milliseconds
 * and tests that entities can be queried by timestamp value.
 *
 * See Mysql57TimestampFspTest for tests using MySQL 5.7. MySQL 5.7 is tested separately
 * because it requires CURRENT_TIMESTAMP(6) or NOW(6) as a default.
 *
 * @author Gail Badner
 */
@SkipForDialect( value={SybaseDialect.class, MySQLDialect.class}, comment="CURRENT_TIMESTAMP not supported as default value in Sybase or MySQL" )
public class TimestampPropertyTest extends BaseCoreFunctionalTestCase {
	private final DateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss.SSS");

	@Test
	public void testTime() {
		final Entity eOrig = new Entity();
		eOrig.ts = new Date();

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( eOrig );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		final Entity eGotten = (Entity) s.get( Entity.class, eOrig.id );
		final String tsOrigFormatted = timestampFormat.format( eOrig.ts );
		final String tsGottenFormatted = timestampFormat.format( eGotten.ts );
		assertEquals( tsOrigFormatted , tsGottenFormatted );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		final Query queryWithParameter = s.createQuery( "from TimestampPropertyTest$Entity where ts=?" ).setParameter( 0, eOrig.ts );
		final Entity eQueriedWithParameter = (Entity) queryWithParameter.uniqueResult();
		assertNotNull( eQueriedWithParameter );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		final Query queryWithTimestamp = s.createQuery( "from TimestampPropertyTest$Entity where ts=?" ).setTimestamp( 0, eOrig.ts );
		final Entity eQueriedWithTimestamp = (Entity) queryWithTimestamp.uniqueResult();
		assertNotNull( eQueriedWithTimestamp );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		s.delete( eQueriedWithTimestamp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testTimeGeneratedByColumnDefault() {
		final Entity eOrig = new Entity();

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( eOrig );
		s.getTransaction().commit();
		s.close();

		assertNotNull( eOrig.tsColumnDefault );

		s = openSession();
		s.getTransaction().begin();
		final Entity eGotten = (Entity) s.get( Entity.class, eOrig.id );
		final String tsColumnDefaultOrigFormatted = timestampFormat.format( eOrig.tsColumnDefault );
		final String tsColumnDefaultGottenFormatted = timestampFormat.format( eGotten.tsColumnDefault );
		assertEquals( tsColumnDefaultOrigFormatted , tsColumnDefaultGottenFormatted );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		final Query queryWithParameter =
				s.createQuery( "from TimestampPropertyTest$Entity where tsColumnDefault=?" )
						.setParameter( 0, eOrig.tsColumnDefault );
		final Entity eQueriedWithParameter = (Entity) queryWithParameter.uniqueResult();
		assertNotNull( eQueriedWithParameter );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		final Query queryWithTimestamp =
				s.createQuery( "from TimestampPropertyTest$Entity where tsColumnDefault=?" )
						.setTimestamp( 0, eOrig.tsColumnDefault );
		final Entity eQueriedWithTimestamp = (Entity) queryWithTimestamp.uniqueResult();
		assertNotNull( eQueriedWithTimestamp );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		s.delete( eQueriedWithTimestamp );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Entity.class };
	}

	@javax.persistence.Entity
	public static class Entity {
		@GeneratedValue
		@Id
		private long id;

		@Temporal( value = TemporalType.TIMESTAMP)
		private Date ts;

		@Temporal( value = TemporalType.TIMESTAMP )
		@Generated( value = GenerationTime.INSERT )
		@ColumnDefault( value = "CURRENT_TIMESTAMP" )
		private Date tsColumnDefault;
	}
}
