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
import org.hibernate.models.Creator;
import org.hibernate.models.spi.MutableAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;

import static org.hibernate.models.spi.AnnotationTarget.Kind;

/**
 * @author Steve Ebersole
 */
public interface XmlAnnotations {
	MutableAnnotationDescriptor<Abstract, AbstractXmlAnnotation> ABSTRACT = Creator.createCompleteAnnotationDescriptor(
			Abstract.class,
			AbstractXmlAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<AnyKeyType, AnyKeyTypeXmlAnnotation> ANY_KEY_TYPE = Creator.createCompleteAnnotationDescriptor(
			AnyKeyType.class,
			AnyKeyTypeXmlAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<CollectionClassification, CollectionClassificationXmlAnnotation> COLLECTION_CLASSIFICATION = Creator.createCompleteAnnotationDescriptor(
			CollectionClassification.class,
			CollectionClassificationXmlAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<Extends, ExtendsXmlAnnotation> EXTENDS = Creator.createCompleteAnnotationDescriptor(
			Extends.class,
			ExtendsXmlAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<Target, TargetXmlAnnotation> TARGET = Creator.createCompleteAnnotationDescriptor(
			Target.class,
			TargetXmlAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<?>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( XmlAnnotations.class, consumer );
	}
}
