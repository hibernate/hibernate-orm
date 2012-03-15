/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.test.annotations.reflection;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.dom4j.io.SAXReader;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotSupportedException;

import org.hibernate.cfg.EJB3DTDEntityResolver;
import org.hibernate.cfg.annotations.reflection.XMLContext;
import org.hibernate.internal.util.xml.ErrorLogger;
import org.hibernate.internal.util.xml.XMLHelper;

/**
 * @author Emmanuel Bernard
 */
public class XMLContextTest {
	@Test
	public void testAll() throws Exception {
		XMLHelper xmlHelper = new XMLHelper();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream is = cl.getResourceAsStream(
				"org/hibernate/test/annotations/reflection/orm.xml"
		);
		Assert.assertNotNull( "ORM.xml not found", is );
		XMLContext context = new XMLContext();
		ErrorLogger errorLogger = new ErrorLogger();
		SAXReader saxReader = xmlHelper.createSAXReader( errorLogger, EJB3DTDEntityResolver.INSTANCE );
		//saxReader.setValidation( false );
		try {
			saxReader.setFeature( "http://apache.org/xml/features/validation/schema", true );
		}
		catch ( SAXNotSupportedException e ) {
			saxReader.setValidation( false );
		}
		org.dom4j.Document doc;
		try {
			doc = saxReader
					.read( new InputSource( new BufferedInputStream( is ) ) );
		}
		finally {
			try {
				is.close();
			}
			catch ( IOException ioe ) {
				//log.warn( "Could not close input stream", ioe );
			}
		}
		Assert.assertFalse( errorLogger.hasErrors() );
		context.addDocument( doc );
	}
}
