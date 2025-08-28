/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpamodelgen;

import org.hibernate.processor.HibernateProcessor;
import org.jspecify.annotations.NonNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.tools.Diagnostic;

import static org.hibernate.processor.HibernateProcessor.ADD_GENERATED_ANNOTATION;
import static org.hibernate.processor.HibernateProcessor.ADD_GENERATION_DATE;
import static org.hibernate.processor.HibernateProcessor.ADD_SUPPRESS_WARNINGS_ANNOTATION;
import static org.hibernate.processor.HibernateProcessor.DEBUG_OPTION;
import static org.hibernate.processor.HibernateProcessor.FULLY_ANNOTATION_CONFIGURED_OPTION;
import static org.hibernate.processor.HibernateProcessor.LAZY_XML_PARSING;
import static org.hibernate.processor.HibernateProcessor.ORM_XML_OPTION;
import static org.hibernate.processor.HibernateProcessor.PERSISTENCE_XML_OPTION;
import static org.hibernate.processor.HibernateProcessor.SUPPRESS_JAKARTA_DATA_METAMODEL;
import static org.hibernate.processor.util.Constants.EMBEDDABLE;
import static org.hibernate.processor.util.Constants.ENTITY;
import static org.hibernate.processor.util.Constants.FIND;
import static org.hibernate.processor.util.Constants.HIB_FETCH_PROFILE;
import static org.hibernate.processor.util.Constants.HIB_FILTER_DEF;
import static org.hibernate.processor.util.Constants.HIB_NAMED_NATIVE_QUERY;
import static org.hibernate.processor.util.Constants.HIB_NAMED_QUERY;
import static org.hibernate.processor.util.Constants.HQL;
import static org.hibernate.processor.util.Constants.JD_REPOSITORY;
import static org.hibernate.processor.util.Constants.MAPPED_SUPERCLASS;
import static org.hibernate.processor.util.Constants.NAMED_ENTITY_GRAPH;
import static org.hibernate.processor.util.Constants.NAMED_NATIVE_QUERY;
import static org.hibernate.processor.util.Constants.NAMED_QUERY;
import static org.hibernate.processor.util.Constants.SQL;
import static org.hibernate.processor.util.Constants.SQL_RESULT_SET_MAPPING;

/**
 * @deprecated Use {@link HibernateProcessor}
 */
@SupportedAnnotationTypes({
		// standard for JPA 2
		ENTITY, MAPPED_SUPERCLASS, EMBEDDABLE,
		// standard for JPA 3.2
		NAMED_QUERY, NAMED_NATIVE_QUERY, NAMED_ENTITY_GRAPH, SQL_RESULT_SET_MAPPING,
		// extra for Hibernate
		HIB_FETCH_PROFILE, HIB_FILTER_DEF, HIB_NAMED_QUERY, HIB_NAMED_NATIVE_QUERY,
		// Hibernate query methods
		HQL, SQL, FIND,
		// Jakarta Data repositories
		JD_REPOSITORY // do not need to list any other Jakarta Data annotations here
})
@SupportedOptions({
		DEBUG_OPTION,
		PERSISTENCE_XML_OPTION,
		ORM_XML_OPTION,
		FULLY_ANNOTATION_CONFIGURED_OPTION,
		LAZY_XML_PARSING,
		ADD_GENERATION_DATE,
		ADD_GENERATED_ANNOTATION,
		ADD_SUPPRESS_WARNINGS_ANNOTATION,
		SUPPRESS_JAKARTA_DATA_METAMODEL
})
@Deprecated(forRemoval = true)
public class JPAMetaModelEntityProcessor extends HibernateProcessor {
	@Override
	public synchronized void init(@NonNull ProcessingEnvironment processingEnvironment) {
		processingEnvironment.getMessager().printMessage(
				Diagnostic.Kind.WARNING,
				"JPAMetaModelEntityProcessor is deprecated, replaced by org.hibernate.processor.HibernateProcessor"
		);
		super.init(processingEnvironment);
	}
}
