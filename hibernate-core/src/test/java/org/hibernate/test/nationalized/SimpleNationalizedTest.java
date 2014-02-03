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
package org.hibernate.test.nationalized;

import java.sql.NClob;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@FailureExpectedWithNewMetamodel
public class SimpleNationalizedTest extends BaseUnitTestCase {

	@Entity( name="NationalizedEntity")
	public static class NationalizedEntity {
		@Id
		private Integer id;

		@Nationalized
		private String nvarcharAtt;

		@Lob
		@Nationalized
		private String materializedNclobAtt;

		@Lob
		@Nationalized
		private NClob nclobAtt;

		@Nationalized
		private Character ncharacterAtt;
		
		@Nationalized
		private Character[] ncharArrAtt;
		
		@Type(type = "ntext")
		private String nlongvarcharcharAtt;
	}

	@Test
	public void simpleNationalizedTest() {
		throw new NotYetImplementedException( "Not implemented using new metamodel." );
//		Configuration cfg = new Configuration();
//		cfg.addAnnotatedClass( NationalizedEntity.class );
//		cfg.buildMappings();
//		PersistentClass pc = cfg.getClassMapping( NationalizedEntity.class.getName() );
//		assertNotNull( pc );
//
//		{
//			Property prop = pc.getProperty( "nvarcharAtt" );
//			assertSame( StringNVarcharType.INSTANCE, prop.getType() );
//		}
//
//		{
//			Property prop = pc.getProperty( "materializedNclobAtt" );
//			assertSame( MaterializedNClobType.INSTANCE, prop.getType() );
//		}
//
//		{
//			Property prop = pc.getProperty( "nclobAtt" );
//			assertSame( NClobType.INSTANCE, prop.getType() );
//		}
//
//		{
//			Property prop = pc.getProperty( "nlongvarcharcharAtt" );
//			assertSame( NTextType.INSTANCE, prop.getType() );
//		}
//
//		{
//			Property prop = pc.getProperty( "ncharArrAtt" );
//			assertSame( StringNVarcharType.INSTANCE, prop.getType() );
//		}
//
//		{
//			Property prop = pc.getProperty( "ncharacterAtt" );
//			assertSame( CharacterNCharType.INSTANCE, prop.getType() );
//		}
	}
}
