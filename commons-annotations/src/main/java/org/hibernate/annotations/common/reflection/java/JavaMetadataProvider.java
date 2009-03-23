package org.hibernate.annotations.common.reflection.java;

import java.util.Map;
import java.util.Collections;
import java.lang.reflect.AnnotatedElement;

import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.AnnotationReader;

/**
 * @author Emmanuel Bernard
*/
public class JavaMetadataProvider implements MetadataProvider {

	public Map<Object, Object> getDefaults() {
		return Collections.emptyMap();
	}

	public AnnotationReader getAnnotationReader(AnnotatedElement annotatedElement) {
		return new JavaAnnotationReader(annotatedElement);
	}
}
