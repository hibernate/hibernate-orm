/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

/**
 * @author Steve Ebersole
 */
public interface JandexAccess {
	/**
	 * The annotation repository that this context know about.
	 *
	 * @return The {@link org.jboss.jandex.IndexView} that this context know about.
	 */
	IndexView getIndex();

	/**
	 * Gets the class (or interface, or annotation) that was scanned during the
	 * indexing phase.
	 *
	 * @param className the name of the class
	 * @return information about the class or null if it is not known
	 */
	ClassInfo getClassInfo(String className);

	/**
	 * Get a type-specific extractor for extracting attribute values from Jandex
	 * AnnotationInstances.
	 *
	 * @param type The type of extractor we want
	 * @param <T> The generic type of the extractor
	 *
	 * @return The typed extractor
	 */
	<T> TypedValueExtractor<T> getTypedValueExtractor(Class<T> type);

}
