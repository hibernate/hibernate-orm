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
import org.hibernate.models.CompleteAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;

import static org.hibernate.models.spi.AnnotationTarget.Kind;

/**
 * @author Steve Ebersole
 */
public interface XmlAnnotations {
	CompleteAnnotationDescriptor<Abstract, AbstractXmlAnnotation> ABSTRACT = new CompleteAnnotationDescriptor<>(
			Abstract.class,
			AbstractXmlAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<AnyKeyType, AnyKeyTypeXmlAnnotation> ANY_KEY_TYPE = new CompleteAnnotationDescriptor<>(
			AnyKeyType.class,
			AnyKeyTypeXmlAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<CollectionClassification, CollectionClassificationXmlAnnotation> COLLECTION_CLASSIFICATION = new CompleteAnnotationDescriptor<>(
			CollectionClassification.class,
			CollectionClassificationXmlAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<Extends, ExtendsXmlAnnotation> EXTENDS = new CompleteAnnotationDescriptor<>(
			Extends.class,
			ExtendsXmlAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<Target, TargetXmlAnnotation> TARGET = new CompleteAnnotationDescriptor<>(
			Target.class,
			TargetXmlAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<?>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( XmlAnnotations.class, consumer );
	}
}
