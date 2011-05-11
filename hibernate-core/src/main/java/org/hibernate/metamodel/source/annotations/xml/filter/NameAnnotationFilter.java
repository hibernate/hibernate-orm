package org.hibernate.metamodel.source.annotations.xml.filter;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * @author Strong Liu
 */
class NameAnnotationFilter extends AbstractAnnotationFilter {
	@Override
	protected void process(DotName annName, AnnotationInstance annotationInstance, List<AnnotationInstance> indexedAnnotationInstanceList) {
		indexedAnnotationInstanceList.clear();
	}

	public static NameTargetAnnotationFilter INSTANCE = new NameTargetAnnotationFilter();

	@Override
	protected DotName[] targetAnnotation() {
		return new DotName[] {
				CACHEABLE,
				TABLE,
				EXCLUDE_DEFAULT_LISTENERS,
				EXCLUDE_SUPERCLASS_LISTENERS,
				ID_CLASS,
				INHERITANCE,
				DISCRIMINATOR_VALUE,
				DISCRIMINATOR_COLUMN,
				ENTITY_LISTENERS
		};
	}

}
