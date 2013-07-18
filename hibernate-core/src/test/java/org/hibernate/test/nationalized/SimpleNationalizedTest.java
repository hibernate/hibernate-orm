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

import org.junit.Test;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestMethod;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.junit4.TestSessionFactoryHelper;
import org.hibernate.type.CharacterNCharType;
import org.hibernate.type.MaterializedNClobType;
import org.hibernate.type.NClobType;
import org.hibernate.type.NTextType;
import org.hibernate.type.StringNVarcharType;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 */
@FailureExpectedWithNewMetamodel
public class SimpleNationalizedTest extends BaseCoreFunctionalTestMethod {

	@Entity(name = "NationalizedEntity")
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
		getTestConfiguration().addAnnotatedClass( NationalizedEntity.class );
		getSessionFactoryHelper().setCallback(
				new TestSessionFactoryHelper.CallbackImpl() {
					@Override
					public void afterConfigurationBuilt(final Configuration configuration) {
						assertConfiguration( configuration );
					}

					@Override
					public void afterMetadataBuilt(final MetadataImplementor metadataImplementor) {
						EntityBinding entityBinding = metadataImplementor.getEntityBinding( NationalizedEntity.class.getName() );
						assertEntitybinding( entityBinding );
					}
				}
		).getSessionFactory();

	}

	private void assertEntitybinding(EntityBinding entityBinding) {
		assertNotNull( entityBinding );
		assertAttributeType( entityBinding, "nvarcharAtt", StringNVarcharType.INSTANCE );
		assertAttributeType( entityBinding, "materializedNclobAtt", MaterializedNClobType.INSTANCE );
		assertAttributeType( entityBinding, "nclobAtt", NClobType.INSTANCE );
		assertAttributeType( entityBinding, "nlongvarcharcharAtt", NTextType.INSTANCE );
		assertAttributeType( entityBinding, "ncharArrAtt", StringNVarcharType.INSTANCE );
		assertAttributeType( entityBinding, "ncharacterAtt", CharacterNCharType.INSTANCE );

	}

	private void assertAttributeType(EntityBinding entityBinding, String attributeName, org.hibernate.type.Type type) {
		AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( attributeName );
		assertSame( attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping(), type );

	}

	private void assertConfiguration(Configuration cfg) {
		PersistentClass pc = cfg.getClassMapping( NationalizedEntity.class.getName() );
		assertNotNull( pc );

		{
			Property prop = pc.getProperty( "nvarcharAtt" );
			assertSame( StringNVarcharType.INSTANCE, prop.getType() );
		}

		{
			Property prop = pc.getProperty( "materializedNclobAtt" );
			assertSame( MaterializedNClobType.INSTANCE, prop.getType() );
		}

		{
			Property prop = pc.getProperty( "nclobAtt" );
			assertSame( NClobType.INSTANCE, prop.getType() );
		}

		{
			Property prop = pc.getProperty( "nlongvarcharcharAtt" );
			assertSame( NTextType.INSTANCE, prop.getType() );
		}

		{
			Property prop = pc.getProperty( "ncharArrAtt" );
			assertSame( StringNVarcharType.INSTANCE, prop.getType() );
		}

		{
			Property prop = pc.getProperty( "ncharacterAtt" );
			assertSame( CharacterNCharType.INSTANCE, prop.getType() );
		}
	}
}
