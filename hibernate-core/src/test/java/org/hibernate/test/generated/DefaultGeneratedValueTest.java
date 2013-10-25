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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * @author Steve Ebersole
 */
public class DefaultGeneratedValueTest extends BaseCoreFunctionalTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-2907" )
	public void testGeneration() {
		Session s = openSession();
		s.beginTransaction();
		TheEntity theEntity = new TheEntity( 1 );
		assertNull( theEntity.createdDate );
		s.save( theEntity );
		assertNull( theEntity.createdDate );
		s.getTransaction().commit();
		s.close();

		assertNotNull( theEntity.createdDate );

		s = openSession();
		s.beginTransaction();
		theEntity = (TheEntity) session.get( TheEntity.class, 1 );
		assertNotNull( theEntity.createdDate );
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

		private TheEntity() {
		}

		private TheEntity(Integer id) {
			this.id = id;
		}
	}

}
