/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

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

/**
 * @author Steve Ebersole
 */
public interface XmlAnnotations {
	OrmAnnotationDescriptor<Abstract, AbstractXmlAnnotation> ABSTRACT = new OrmAnnotationDescriptor<>(
			Abstract.class,
			AbstractXmlAnnotation.class
	);
	OrmAnnotationDescriptor<AnyKeyType, AnyKeyTypeXmlAnnotation> ANY_KEY_TYPE = new OrmAnnotationDescriptor<>(
			AnyKeyType.class,
			AnyKeyTypeXmlAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionClassification, CollectionClassificationXmlAnnotation> COLLECTION_CLASSIFICATION = new OrmAnnotationDescriptor<>(
			CollectionClassification.class,
			CollectionClassificationXmlAnnotation.class
	);
	OrmAnnotationDescriptor<Extends, ExtendsXmlAnnotation> EXTENDS = new OrmAnnotationDescriptor<>(
			Extends.class,
			ExtendsXmlAnnotation.class
	);
	OrmAnnotationDescriptor<Target, TargetXmlAnnotation> TARGET = new OrmAnnotationDescriptor<>(
			Target.class,
			TargetXmlAnnotation.class
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<?>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( XmlAnnotations.class, consumer );
	}
}
