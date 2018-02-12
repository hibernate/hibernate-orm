/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.schema;

import org.hibernate.annotations.common.reflection.XClass;

/**
 * SchemaNamingProvider singleton locator.<br>
 *
 * TODO : integrate into class AnnotationSettings as "official" configuration. <br>
 * TODO (later) : migrate into a non static approach... (complex)
 *
 * @author Benoit Besson
 */
public class SchemaNamingProviderLocator {

	/** static instance to delegate on. */
	private static SchemaNamingProvider instance;

	public static void setInstance(SchemaNamingProvider instance) {
		SchemaNamingProviderLocator.instance = instance;
	}

	public static SchemaNamingProvider getInstance() {
		return instance;
	}

	// called each time the processing class has changed
	public static void setCurrentProcessingClass(XClass xClass) {
		// do nothing is instance is not set, else delegate
		if (instance != null) {
			instance.setCurrentProcessingClass(xClass);
		}
	}

	// ask for dynamic schema name for table
	public static String resolveSchemaName(String annotationSchemaName,
			String annotationTableName) {
		// do nothing is instance is not set, else delegate
		if (instance != null) {
			return instance.resolveSchemaName(annotationSchemaName, annotationTableName);
		}
		return annotationSchemaName;
	}

	// ask for dynamic schema name for sequence
	public static String resolveSequenceName(String annotationSequenceName) {
		// do nothing is instance is not set, else delegate
		if (instance != null) {
			return instance.resolveSequenceName(annotationSequenceName);
		}
		return annotationSequenceName;
	}
}
