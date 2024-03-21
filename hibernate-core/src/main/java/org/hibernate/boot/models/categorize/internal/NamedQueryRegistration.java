/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.AnnotationUsage;

/**
 * @see JpaAnnotations#NAMED_QUERY
 * @see JpaAnnotations#NAMED_NATIVE_QUERY
 * @see JpaAnnotations#NAMED_STORED_PROCEDURE_QUERY
 * @see HibernateAnnotations#NAMED_QUERY
 * @see HibernateAnnotations#NAMED_NATIVE_QUERY
 *
 * @author Steve Ebersole
 */
public class NamedQueryRegistration {
	public enum Kind {
		HQL,
		NATIVE,
		CALLABLE
	}

	private final String name;
	private final Kind kind;
	private final boolean isJpa;
	private final AnnotationUsage<? extends Annotation> configuration;

	public NamedQueryRegistration(String name, Kind kind, boolean isJpa, AnnotationUsage<? extends Annotation> configuration) {
		this.name = name;
		this.kind = kind;
		this.isJpa = isJpa;
		this.configuration = configuration;
	}

	public String getName() {
		return name;
	}

	public Kind getKind() {
		return kind;
	}

	public boolean isJpa() {
		return isJpa;
	}

	public AnnotationUsage<? extends Annotation> getConfiguration() {
		return configuration;
	}
}
