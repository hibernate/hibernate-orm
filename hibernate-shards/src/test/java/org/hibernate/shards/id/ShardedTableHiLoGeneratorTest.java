/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.id;

import static org.junit.Assert.*;

import org.hibernate.Session;
import org.hibernate.TestingDatabaseInfo;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.TableGenerator;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jdbc.Work;
import org.hibernate.mapping.SimpleAuxiliaryDatabaseObject;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.shards.engine.ShardedSessionFactoryImplementor;
import org.hibernate.shards.session.ControlSessionProvider;
import org.hibernate.shards.session.ShardedSessionImpl;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.type.StandardBasicTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Got part of this code from TableHiLoGeneratorTest.
 *
 * @author maxr@google.com (Max Ross)
 * @author Adriano Machado
 */
public class ShardedTableHiLoGeneratorTest extends BaseUnitTestCase {

    private static final String GEN_TABLE = "generator_table";
    private static final String GEN_COLUMN = ShardedTableHiLoGenerator.DEFAULT_COLUMN_NAME;

    private Configuration cfg;
    private ServiceRegistry serviceRegistry;
    private ShardedSessionFactoryImplementor sessionFactory;
    private ShardedTableHiLoGenerator generator;

    @Before
    public void setUp() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty( TableGenerator.TABLE, GEN_TABLE );
        properties.setProperty( TableGenerator.COLUMN, GEN_COLUMN );
        properties.setProperty( ShardedTableHiLoGenerator.MAX_LO, "3" );
        properties.put(
                PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
                new ObjectNameNormalizer() {
                    @Override
                    protected boolean isUseQuotedIdentifiersGlobally() {
                        return false;
                    }

                    @Override
                    protected NamingStrategy getNamingStrategy() {
                        return cfg.getNamingStrategy();
                    }
                }
        );

        final Dialect dialect = new H2Dialect();

        generator = new ShardedTableHiLoGenerator();
        generator.configure( StandardBasicTypes.LONG, properties, dialect );

        cfg = TestingDatabaseInfo.buildBaseConfiguration()
                .setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
        cfg.addAuxiliaryDatabaseObject(
                new SimpleAuxiliaryDatabaseObject(
                        generator.sqlCreateStrings( dialect )[0],
                        generator.sqlDropStrings( dialect )[0]
                )
        );

        cfg.addAuxiliaryDatabaseObject(
                new SimpleAuxiliaryDatabaseObject(
                        generator.sqlCreateStrings( dialect )[1],
                        null
                )
        );

        serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry(cfg.getProperties());
        sessionFactory = (ShardedSessionFactoryImplementor) cfg.buildSessionFactory( serviceRegistry );
    }

    @After
    public void tearDown() throws Exception {
        if ( sessionFactory != null ) {
            sessionFactory.close();
        }
        if ( serviceRegistry != null ) {
            ServiceRegistryBuilder.destroy( serviceRegistry );
        }
    }

    @Test
    public void testShardedHiLoAlgorithm() {
        final ShardedSessionImpl session = (ShardedSessionImpl) sessionFactory.openSession();
        final ControlSessionProvider provider = new ControlSessionProvider() {
            public SessionImplementor openControlSession() {
                return session;
            }
        };

        generator.setControlSessionProvider(provider);
        session.beginTransaction();

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // initially sequence should be uninitialized
        assertEquals( 0L, extractInDatabaseValue( session ) );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        Long generatedValue = (Long) generator.generate( session, null );
        assertEquals( 1L, generatedValue.longValue() );
        assertEquals( 1L, extractInDatabaseValue( session ) );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        generatedValue = (Long) generator.generate( session, null );
        assertEquals( 2L, generatedValue.longValue() );
        assertEquals( 1L, extractInDatabaseValue( session ) );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        generatedValue = (Long) generator.generate( session, null );
        assertEquals( 3L, generatedValue.longValue() );
        assertEquals( 1L, extractInDatabaseValue( session ) );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        generatedValue = (Long) generator.generate( session, null );
        assertEquals( 4L, generatedValue.longValue() );
        assertEquals( 2L, extractInDatabaseValue( session ) );

        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        generatedValue = (Long) generator.generate( session, null );
        assertEquals( 5L, generatedValue.longValue() );
        assertEquals( 2L, extractInDatabaseValue( session ) );

        session.getTransaction().commit();
        session.close();
    }

    private long extractInDatabaseValue(final Session session) {
        class WorkImpl implements Work {
            private long value;
            public void execute(final Connection connection) throws SQLException {
                PreparedStatement query = connection.prepareStatement( "select " + GEN_COLUMN + " from " + GEN_TABLE );
                ResultSet resultSet = query.executeQuery();
                resultSet.next();
                value = resultSet.getLong( 1 );
            }
        }
        final WorkImpl work = new WorkImpl();
        session.doWork( work );
        return work.value;
    }

    /*
    @Test
    public void testGenerate() {
        final SessionImplementor controlSessionToReturn = sessionFactory.openSession();
        final ControlSessionProvider provider = new ControlSessionProvider() {
            public SessionImplementor openControlSession() {
                return controlSessionToReturn;
            }
        };
        final SessionImplementor session = (SessionImplementor)sessionFactory().openSession();
        final ShardedTableHiLoGenerator gen = new ShardedTableHiLoGenerator() {
            @Override
            Serializable superGenerate(final SessionImplementor controlSession, final Object obj) {
                assertSame(controlSessionToReturn, controlSession);
                return 33;
            }
        };
        gen.setControlSessionProvider(provider);
        assertEquals(33, gen.generate(session, null));
    }
    */
}
