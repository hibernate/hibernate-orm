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
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.CharacterNCharType;
import org.hibernate.type.MaterializedNClobType;
import org.hibernate.type.NClobType;
import org.hibernate.type.NTextType;
import org.hibernate.type.StringNVarcharType;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 */
public class SimpleNationalizedTest extends BaseUnitTestCase {

	@SuppressWarnings({"UnusedDeclaration", "SpellCheckingInspection"})
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
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( NationalizedEntity.class );

			final Metadata metadata = ms.buildMetadata();
			PersistentClass pc = metadata.getEntityBinding( NationalizedEntity.class.getName() );
			assertNotNull( pc );

			Property prop = pc.getProperty( "nvarcharAtt" );
			assertSame( StringNVarcharType.INSTANCE, prop.getType() );

			prop = pc.getProperty( "materializedNclobAtt" );
			assertSame( MaterializedNClobType.INSTANCE, prop.getType() );

			prop = pc.getProperty( "nclobAtt" );
			assertSame( NClobType.INSTANCE, prop.getType() );

			prop = pc.getProperty( "nlongvarcharcharAtt" );
			assertSame( NTextType.INSTANCE, prop.getType() );

			prop = pc.getProperty( "ncharArrAtt" );
			assertSame( StringNVarcharType.INSTANCE, prop.getType() );

			prop = pc.getProperty( "ncharacterAtt" );
			assertSame( CharacterNCharType.INSTANCE, prop.getType() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
