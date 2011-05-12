/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.source.annotations.xml.filter;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.xml.mocker.IndexBuilder;

/**
 * @author Strong Liu
 */
public interface IndexedAnnotationFilter extends JPADotNames {
	DotName[] GLOBAL_ANNOTATIONS = new DotName[] {
			SEQUENCE_GENERATOR,
			TABLE_GENERATOR,
			NAMED_QUERIES,
			NAMED_QUERY,
			NAMED_NATIVE_QUERIES,
			NAMED_NATIVE_QUERY,
			SQL_RESULT_SET_MAPPING,
			SQL_RESULT_SET_MAPPINGS
	};
	DotName[] SCHEMAAWARE_ANNOTATIONS = new DotName[] {
			TABLE, JOIN_TABLE, COLLECTION_TABLE, SECONDARY_TABLE, SECONDARY_TABLES
			, TABLE_GENERATOR, SEQUENCE_GENERATOR
	};
	DotName[] ASSOCIATION_ANNOTATIONS = new DotName[] {
			ONE_TO_MANY, ONE_TO_ONE, MANY_TO_ONE, MANY_TO_MANY
	};
	IndexedAnnotationFilter[] filters = new IndexedAnnotationFilter[] {
			ExclusiveAnnotationFilter.INSTANCE,
			NameAnnotationFilter.INSTANCE, NameTargetAnnotationFilter.INSTANCE
	};


	void beforePush(IndexBuilder indexBuilder, DotName classDotName, AnnotationInstance annotationInstance);
}
