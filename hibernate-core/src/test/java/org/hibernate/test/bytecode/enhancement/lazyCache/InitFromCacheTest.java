/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazyCache;

import java.util.Date;
import java.util.Locale;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Formula;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@RunWith( BytecodeEnhancerRunner.class )
public class InitFromCacheTest extends BaseCoreFunctionalTestCase {

    private EntityPersister persister;

    private Long documentID;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class[]{Document.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
        configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
    }

    @Before
    public void prepare() {
        persister = sessionFactory().getMetamodel().entityPersister( Document.class );
        assertTrue( persister.hasCache() );

        doInHibernate( this::sessionFactory, s -> {
            Document document = new Document( "HiA", "Hibernate book", "Hibernate is...." );
            s.persist( document );
            documentID = document.id;
        } );
    }

    @Test
    public void execute() {

        doInHibernate(
        		this::sessionFactory,
				s -> {
					Document d = (Document) s.createQuery( "from Document fetch all properties" ).uniqueResult();
					assertTrue( isPropertyInitialized( d, "text" ) );
					assertTrue( isPropertyInitialized( d, "summary" ) );

					final EntityDataAccess entityDataAccess = persister.getCacheAccessStrategy();
					final Object cacheKey = entityDataAccess.generateCacheKey(
							d.id,
							persister,
							sessionFactory(),
							null
					);
					final Object cachedItem = entityDataAccess.get( (SharedSessionContractImplementor) s, cacheKey );
					assertNotNull( cachedItem );
					assertTyping( StandardCacheEntryImpl.class, cachedItem );
                }
        );

        sessionFactory().getStatistics().clear();

        doInHibernate( this::sessionFactory, s -> {
            Document d = (Document) s.createCriteria( Document.class ).uniqueResult();
            assertFalse( isPropertyInitialized( d, "text" ) );
            assertFalse( isPropertyInitialized( d, "summary" ) );
            assertEquals( "Hibernate is....", d.text );
            assertTrue( isPropertyInitialized( d, "text" ) );
            assertTrue( isPropertyInitialized( d, "summary" ) );
        } );

        assertEquals( 2, sessionFactory().getStatistics().getPrepareStatementCount() );

        doInHibernate( this::sessionFactory, s -> {
            Document d = s.get( Document.class, documentID );
            assertFalse( isPropertyInitialized( d, "text" ) );
            assertFalse( isPropertyInitialized( d, "summary" ) );
        } );
    }

    // --- //

    @Entity( name = "Document" )
    @Table( name = "DOCUMENT" )
    @Cacheable
    @Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy", region = "foo" )
    private static class Document {

        @Id
        @GeneratedValue
        Long id;

        String name;

        @Basic( fetch = FetchType.LAZY )
        @Formula( "upper(name)" )
        String upperCaseName;

        @Basic( fetch = FetchType.LAZY )
        String summary;

        @Basic( fetch = FetchType.LAZY )
        String text;

        @Basic( fetch = FetchType.LAZY )
        Date lastTextModification;

        Document() {
        }

        Document(String name, String summary, String text) {
            this.lastTextModification = new Date();
            this.name = name;
            this.upperCaseName = name.toUpperCase( Locale.ROOT );
            this.summary = summary;
            this.text = text;
        }
    }
}
