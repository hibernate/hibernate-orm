/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.typeoverride;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 *
 * @author Gail Badner
 */
public class TypeOverrideTest extends FunctionalTestCase {

	public TypeOverrideTest(String string)  {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "typeoverride/Entity.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.registerTypeOverride( StoredPrefixedStringType.INSTANCE );
	}

	public void testStandardBasicSqlTypeDescriptor() {
		// no override
		assertTrue( StandardBasicTypes.isStandardBasicSqlTypeDescriptor( IntegerTypeDescriptor.INSTANCE ) );
		assertSame( IntegerTypeDescriptor.INSTANCE, getResolvedSqlTypeDescriptor( IntegerTypeDescriptor.INSTANCE ) );

		// override depends on Dialect.useInputStreamToInsertBlob();
		// Postgresql explicitly overrides BlobTypeDescriptor.DEFAULT
		assertTrue( StandardBasicTypes.isStandardBasicSqlTypeDescriptor( BlobTypeDescriptor.DEFAULT ) );
		if ( getDialect().useInputStreamToInsertBlob() ) {
			assertSame(
					BlobTypeDescriptor.STREAM_BINDING,
					getDialect().resolveSqlTypeDescriptor( BlobTypeDescriptor.DEFAULT )
			);
		}
		else if ( PostgreSQLDialect.class.isInstance( getDialect() ) )  {
			assertSame(
					BlobTypeDescriptor.BLOB_BINDING,
					getDialect().resolveSqlTypeDescriptor( BlobTypeDescriptor.DEFAULT )
			);
		}
		else {
			assertSame(
					BlobTypeDescriptor.DEFAULT,
					getDialect().resolveSqlTypeDescriptor( BlobTypeDescriptor.DEFAULT )
			);
		}
	}

	public void testNonStandardSqlTypeDescriptor() {
		// no override
		SqlTypeDescriptor sqlTypeDescriptor = new IntegerTypeDescriptor();
		assertFalse( StandardBasicTypes.isStandardBasicSqlTypeDescriptor( sqlTypeDescriptor ) );
		assertSame( sqlTypeDescriptor, getResolvedSqlTypeDescriptor( sqlTypeDescriptor ) );

		// no override; (ClobTypeDescriptor.DEFAULT	is overridden
		// if Dialect.useInputStreamToInsertBlob() is true)
		assertFalse( StandardBasicTypes.isStandardBasicSqlTypeDescriptor( ClobTypeDescriptor.CLOB_BINDING ) );
		assertSame( ClobTypeDescriptor.CLOB_BINDING, getResolvedSqlTypeDescriptor( ClobTypeDescriptor.CLOB_BINDING ) );
	}

	public void testDialectWithNonStandardSqlTypeDescriptor() {
		assertNotSame( VarcharTypeDescriptor.INSTANCE, StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor() );
		if ( H2DialectOverridePrefixedVarcharSqlTypeDesc.class.isInstance( getDialect() ) ) {
			// TODO: dialect is currently a global; how can this be tested in the testsuite?
			assertSame(
					VarcharTypeDescriptor.INSTANCE,
					getResolvedSqlTypeDescriptor( StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor() )
			);
		}
		else {
			assertSame(
					StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor(),
					getResolvedSqlTypeDescriptor( StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor() )
			);
		}

		if ( H2DialectOverrideVarcharSqlCode.class.isInstance( getDialect() ) ) {
			// TODO: dialect is currently a global; how can this be tested in the testsuite?
			assertSame(
					StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor(),
					getResolvedSqlTypeDescriptor( VarcharTypeDescriptor.INSTANCE )
			);
		}
		else {
			assertSame(
					VarcharTypeDescriptor.INSTANCE,
					getResolvedSqlTypeDescriptor( VarcharTypeDescriptor.INSTANCE )
			);
		}
	}

	private SqlTypeDescriptor getResolvedSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		return ( ( SessionFactoryImplementor ) getSessions() )
				.getTypeResolver()
				.resolveSqlTypeDescriptor( sqlTypeDescriptor );
	}

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
		String trimmedName = ( String ) s.createQuery( "select trim( TRAILING from e.name ) from Entity e" ).uniqueResult();
		// trim(...) is a "standard" DB function returning VarcharTypeDescriptor.INSTANCE,
		// so the prefix will not be removed unless
		// 1) getDialect().getSqlTypeDescriptorOverride( VarcharTypeDescriptor.INSTANCE )
		// returns StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor()
		// (H2DialectOverrideVarcharSqlCode does this)
		// or 2) getDialect().getSqlTypeDescriptorOverride( StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor() )
		// returns VarcharTypeDescriptor.INSTANCE
		// (H2DialectOverridePrefixedVarcharSqlTypeDesc does this)
		// TODO: dialect is currently a global; how can this be tested in the testsuite?
		assertNotSame( VarcharTypeDescriptor.INSTANCE, StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor() );
		if ( getDialect().resolveSqlTypeDescriptor( VarcharTypeDescriptor.INSTANCE ) ==
				StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor() ||
				getDialect().resolveSqlTypeDescriptor( StoredPrefixedStringType.INSTANCE.getSqlTypeDescriptor() ) ==
						VarcharTypeDescriptor.INSTANCE ) {
			assertFalse( trimmedName.startsWith( StoredPrefixedStringType.PREFIX ) );
			assertEquals( "name", trimmedName );
		}
		else {
			assertSame(
					VarcharTypeDescriptor.INSTANCE,
					( ( SessionFactoryImplementor ) getSessions() )
							.getTypeResolver()
							.resolveSqlTypeDescriptor( VarcharTypeDescriptor.INSTANCE )
			);
			assertTrue( trimmedName.startsWith( StoredPrefixedStringType.PREFIX ) );
			assertEquals( StoredPrefixedStringType.PREFIX + "name", trimmedName );
		}
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}
}





