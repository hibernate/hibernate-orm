/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.typeoverride;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;
import org.hibernate.type.descriptor.sql.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;
import org.junit.Test;

/**
 * @author Gail Badner
 */
public class TypeOverrideTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "typeoverride/Entity.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.registerTypeOverride( StoredPrefixedStringType.INSTANCE );
	}

	@Test
	public void testStandardBasicSqlTypeDescriptor() {
		// no override
		assertSame( IntegerTypeDescriptor.INSTANCE, remapSqlTypeDescriptor( IntegerTypeDescriptor.INSTANCE ) );

		// A few dialects explicitly override BlobTypeDescriptor.DEFAULT
		if ( PostgreSQL81Dialect.class.isInstance( getDialect() ) || PostgreSQLDialect.class.isInstance( getDialect() ) )  {
			assertSame(
					BlobTypeDescriptor.BLOB_BINDING,
					getDialect().remapSqlTypeDescriptor( BlobTypeDescriptor.DEFAULT )
			);
		}
		else if (SybaseDialect.class.isInstance( getDialect() )) {
			assertSame(
					BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING,
					getDialect().remapSqlTypeDescriptor( BlobTypeDescriptor.DEFAULT )
			);
		}
		else {
			assertSame(
					BlobTypeDescriptor.DEFAULT,
					getDialect().remapSqlTypeDescriptor( BlobTypeDescriptor.DEFAULT )
			);
		}
	}

	@Test
	public void testNonStandardSqlTypeDescriptor() {
		// no override
		SqlTypeDescriptor sqlTypeDescriptor = new IntegerTypeDescriptor() {
			@Override
			public boolean canBeRemapped() {
				return false;
			}
		};
		assertSame( sqlTypeDescriptor, remapSqlTypeDescriptor( sqlTypeDescriptor ) );
	}

	@Test
	public void testDialectWithNonStandardSqlTypeDescriptor() {
		assertNotSame( VarcharTypeDescriptor.INSTANCE, StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor() );
		final Dialect dialect = new H2DialectOverridePrefixedVarcharSqlTypeDesc();
		final SqlTypeDescriptor remapped = remapSqlTypeDescriptor( dialect, StoredPrefixedStringType.PREFIXED_VARCHAR_TYPE_DESCRIPTOR );
		assertSame( VarcharTypeDescriptor.INSTANCE, remapped );
	}

	private SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		return remapSqlTypeDescriptor( sessionFactory().getDialect(), sqlTypeDescriptor );
	}

	private SqlTypeDescriptor remapSqlTypeDescriptor(Dialect dialect, SqlTypeDescriptor sqlTypeDescriptor) {
		return dialect.remapSqlTypeDescriptor( sqlTypeDescriptor );
	}

	@Test
	public void testInsert() {
		Session s = openSession();
		s.getTransaction().begin();
		Entity e = new Entity( "name" );
		s.save( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = ( Entity ) s.get( Entity.class, e.getId() );
		assertFalse( e.getName().startsWith( StoredPrefixedStringType.PREFIX ) );
		assertEquals( "name", e.getName() );
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	@Test
    @SkipForDialect( value = SybaseASE15Dialect.class, jiraKey = "HHH-6426")
	public void testRegisteredFunction() {
		Session s = openSession();
		s.getTransaction().begin();
		Entity e = new Entity( "name " );
		s.save( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		e = ( Entity ) s.get( Entity.class, e.getId() );
		assertFalse( e.getName().startsWith( StoredPrefixedStringType.PREFIX ) );
		assertEquals( "name ", e.getName() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}
}





