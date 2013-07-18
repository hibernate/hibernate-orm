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
package org.hibernate.test.lob;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.util.Random;

import org.hibernate.LobHelper;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
@TestForIssue( jiraKey = "HHH-7698" )
@RequiresDialect( value = H2Dialect.class, jiraKey = "HHH-7724" )
public class JpaLargeBlobTest extends BaseCoreFunctionalTestCase {
    
    @Override
    protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { LobEntity.class };
	}

    @Override
    protected void configure(Configuration configuration) {
    	super.configure( configuration );
        configuration.setProperty(Environment.USE_STREAMS_FOR_BINARY, "true");
    }

    @Test
    public void jpaBlobStream() throws Exception {
        Session session = openSession();
        LobEntity o = new LobEntity();

        LobHelper lh = session.getLobHelper();
        LobInputStream lis = new LobInputStream();

        session.getTransaction().begin();

        Blob blob = lh.createBlob(lis, LobEntity.BLOB_LENGTH);
        o.setBlob(blob);
        
        // Regardless if NON_CONTEXTUAL_LOB_CREATION is set to true,
        // ContextualLobCreator should use a NonContextualLobCreator to create
        // a blob Proxy.  If that's the case, the InputStream will not be read
        // until it's persisted with the JDBC driver.
        // Although HHH-7698 was about high memory consumption, this is the best
        // way to test that the high memory use is being prevented.
        assertFalse( lis.wasRead() );

        session.persist(o);
        session.getTransaction().commit();
        
        assertTrue( lis.wasRead() );
        
        session.close();

        lis.close();
    }
    
    private class LobInputStream extends InputStream {
    	private boolean read = false;
    	private Long count = (long) 200 * 1024 * 1024;

        @Override
        public int read() throws IOException {
        	read = true;
            if (count > 0) {
                count--;
                return new Random().nextInt();
            }
            return -1;
        }
        
        @Override
        public int available() throws IOException {
        	return 1;
        }
        
        public boolean wasRead() {
        	return read;
        }
    }
}
