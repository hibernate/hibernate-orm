//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.lob;

import java.sql.Clob;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * This class extends AbstractClobTest so that LOBs are created using the
 * Hibernate.createClob() APIs. These APIs do not use the connection to
 * create LOBs.
 *
 * @author Steve Ebersole
 */
public class ClobTest extends AbstractClobTest {

	public ClobTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ClobTest.class );
	}

	protected Clob createClobLocator(Session s, String str) {
		return Hibernate.createClob( str );
	}

	protected Clob createClobLocatorFromStreamUsingLength(Session s, String str) throws IOException {
		return Hibernate.createClob( new StringReader( str ), str.length() );
	}
}