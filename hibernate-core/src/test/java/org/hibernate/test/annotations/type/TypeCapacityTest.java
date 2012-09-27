/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.annotations.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
// TODO: May eventually test other dialects.
@RequiresDialect( DB2Dialect.class )
public class TypeCapacityTest extends BaseCoreFunctionalTestCase {
	
	/**
	 * Dialects use different SQL types based on max capacities.  Ensure
	 * DB2 VARBINARY uses varchar for < 32766 and blob for > 32766;
	 */
	@Test
	@TestForIssue( jiraKey="HHH-4098" )
	public void testVarbinaryCapacity() {
		Column varbinary = getColumn( "varbinary" );
		assertNotNull(varbinary);
		assertEquals(varbinary.getSqlType(), "varchar(32765) for bit data");
		
		Column varbinaryBlob = getColumn( "varbinaryBlob" );
		assertNotNull(varbinaryBlob);
		assertEquals(varbinaryBlob.getSqlType(), "blob(32767)");
		
	}
	
	private Column getColumn( String name) {
		Iterator iter = configuration().createMappings().getTable( 
				null, null, "TypeCapacityTest$TypeCapacityEntity" )
						.getColumnIterator();
		
		while ( iter.hasNext() ) {
			Column c = (Column) iter.next();
			if (c.getName().equals( name )) {
				return c;
			}
		}
		return null;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TypeCapacityEntity.class };
	}
	
	@Entity
	public class TypeCapacityEntity {
		
		@Id
		@GeneratedValue
		private Long id;
		
		@Type( type="org.hibernate.type.BinaryType" )
		@javax.persistence.Column( length = 32765)
		private byte[] varbinary;
		
		@Type( type="org.hibernate.type.BinaryType" )
		@javax.persistence.Column( length = 32767)
		private byte[] varbinaryBlob;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public byte[] getVarbinary() {
			return varbinary;
		}

		public void setVarbinary(byte[] varbinary) {
			this.varbinary = varbinary;
		}

		public byte[] getVarbinaryBlob() {
			return varbinaryBlob;
		}

		public void setVarbinaryBlob(byte[] varbinaryBlob) {
			this.varbinaryBlob = varbinaryBlob;
		}
	}
}
