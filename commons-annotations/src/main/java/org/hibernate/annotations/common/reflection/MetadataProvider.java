package org.hibernate.annotations.common.reflection;

import java.util.Map;
import java.lang.reflect.AnnotatedElement;

/**
 * Provides metadata
 *
 * @author Emmanuel Bernard
 */
public interface MetadataProvider {

	/**
	 * provide default metadata
	 */
	Map<Object, Object> getDefaults();

	/**
	 * provide metadata for a gien annotated element
	 */
	AnnotationReader getAnnotationReader(AnnotatedElement annotatedElement);
}
