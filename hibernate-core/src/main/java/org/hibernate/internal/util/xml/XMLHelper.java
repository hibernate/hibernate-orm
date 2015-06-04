/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.xml;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

import org.dom4j.DocumentFactory;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;

/**
 * Small helper class that lazy loads DOM and SAX reader and keep them for fast use afterwards.
 *
 * @deprecated Currently only used for integration with HCANN.  The rest of Hibernate uses StAX now
 * for XML processing.  See {@link org.hibernate.boot.jaxb.internal.stax}
 */
@Deprecated
public final class XMLHelper {
	private final DocumentFactory documentFactory;

	public XMLHelper(ClassLoaderService classLoaderService) {
		this.documentFactory = classLoaderService.workWithClassLoader(
				new ClassLoaderService.Work<DocumentFactory>() {
					@Override
					public DocumentFactory doWork(ClassLoader classLoader) {
						final ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
						try {
							Thread.currentThread().setContextClassLoader( classLoader );
							return DocumentFactory.getInstance();
						}
						finally {
							Thread.currentThread().setContextClassLoader( originalTccl );
						}
					}
				}
		);

	}

	public DocumentFactory getDocumentFactory() {
		return documentFactory;
	}

	public SAXReader createSAXReader(ErrorLogger errorLogger, EntityResolver entityResolver) {
		SAXReader saxReader = new SAXReader();
		saxReader.setMergeAdjacentText( true );
		saxReader.setValidation( true );
		saxReader.setErrorHandler( errorLogger );
		saxReader.setEntityResolver( entityResolver );

		return saxReader;
	}
}
