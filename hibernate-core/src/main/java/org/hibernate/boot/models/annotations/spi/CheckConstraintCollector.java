/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

import jakarta.persistence.CheckConstraint;

/**
 * Commonality for annotations which define check-constraints
 *
 * @author Steve Ebersole
 */
public interface CheckConstraintCollector extends Annotation {
	CheckConstraint[] check();

	void check(CheckConstraint[] value);
}
