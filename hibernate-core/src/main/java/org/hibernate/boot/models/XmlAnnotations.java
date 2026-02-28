/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.util.EnumSet;
import java.util.function.Consumer;

import org.hibernate.boot.internal.Abstract;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.internal.Extends;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.models.annotations.internal.AbstractXmlAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyKeyTypeXmlAnnotation;
import org.hibernate.boot.models.annotations.internal.CollectionClassificationXmlAnnotation;
import org.hibernate.boot.models.annotations.internal.ExtendsXmlAnnotation;
import org.hibernate.boot.models.annotations.internal.TargetXmlAnnotation;
import org.hibernate.boot.models.internal.OrmAnnotationHelper;
import org.hibernate.models.internal.OrmAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;

import static org.hibernate.models.spi.AnnotationTarget.Kind;

/**
 * @author Steve Ebersole
 */
public interface XmlAnnotations {
	OrmAnnotationDescriptor<Abstract, AbstractXmlAnnotation> ABSTRACT = new OrmAnnotationDescriptor<>(
			Abstract.class,
			AbstractXmlAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<AnyKeyType, AnyKeyTypeXmlAnnotation> ANY_KEY_TYPE = new OrmAnnotationDescriptor<>(
			AnyKeyType.class,
			AnyKeyTypeXmlAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<CollectionClassification, CollectionClassificationXmlAnnotation> COLLECTION_CLASSIFICATION = new OrmAnnotationDescriptor<>(
			CollectionClassification.class,
			CollectionClassificationXmlAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<Extends, ExtendsXmlAnnotation> EXTENDS = new OrmAnnotationDescriptor<>(
			Extends.class,
			ExtendsXmlAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<Target, TargetXmlAnnotation> TARGET = new OrmAnnotationDescriptor<>(
			Target.class,
			TargetXmlAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<?>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( XmlAnnotations.class, consumer );
	}
}
