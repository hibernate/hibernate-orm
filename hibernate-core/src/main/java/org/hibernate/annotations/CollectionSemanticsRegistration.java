/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

import org.hibernate.metamodel.CollectionClassification;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows to register a {@link org.hibernate.collection.spi.CollectionSemantics}
 * to use as the default for the specified classification of collection.
 *
 * @see CollectionClassificationType
 * @see CollectionSemantics
 * @see org.hibernate.collection.spi.CollectionSemantics
 *
 * @since 6.0
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Repeatable( CollectionSemanticsRegistrations.class )
public @interface CollectionSemanticsRegistration {
	/**
	 * The collection classification for which the supplied semantic applies
	 */
	CollectionClassification classification();

	/**
	 * The semantic to apply.  Will be applied to all collections of the given
	 * classification which do not define an explicit {@link org.hibernate.collection.spi.CollectionSemantics}
	 */
	Class<? extends org.hibernate.collection.spi.CollectionSemantics<?,?>> semantics();
}
