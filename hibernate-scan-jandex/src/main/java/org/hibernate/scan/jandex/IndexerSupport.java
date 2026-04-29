/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.scan.jandex;

import org.hibernate.HibernateException;
import org.hibernate.boot.models.internal.OrmAnnotationHelper;
import org.jboss.jandex.Indexer;

import java.io.IOException;

/// Support for dealing with the Jandex [Indexer].
///
/// @author Steve Ebersole
public class IndexerSupport {

	/// Build a Jandex [Indexer] with Hibernate and Jandex annotations applied.
	public static Indexer buildBaselineIndexer() {
		var indexer = new Indexer();
		OrmAnnotationHelper.forEachOrmAnnotation( (descriptor) -> {
			indexClass( descriptor.getAnnotationType(), indexer );
		} );
		return indexer;
	}

	/// Apply the class to the indexer, handling exceptions.
	///
	/// @throws HibernateException If there was a problem indexing the class.
	public static void indexClass(Class<?> clazz, Indexer indexer) {
		try {
			indexer.indexClass( clazz );
		}
		catch (IOException e) {
			throw new HibernateException( "Error indexing class for Jandex index - " + clazz.getName(), e );
		}
	}
}
