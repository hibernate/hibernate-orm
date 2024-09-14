/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

import jakarta.persistence.Index;

/**
 * Commonality for annotations which define indexes
 *
 * @author Steve Ebersole
 */
public interface IndexCollector extends Annotation {
	Index[] indexes();

	void indexes(Index[] value);
}
