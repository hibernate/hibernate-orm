package org.hibernate.metamodel.source.annotations;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AssertionFailure;

/**
 * @author Hardy Ferentschik
 */
public class JandexHelper {
	private JandexHelper() {
	}

	/**
	 * @param classInfo the class info from which to retrieve the annotation instance
	 * @param annotationName the annotation to retrieve from the class info
	 *
	 * @return the single annotation defined on the class or {@code null} in case the annotation is not specified at all
	 *
	 * @throws org.hibernate.AssertionFailure in case there is
	 */
	static public AnnotationInstance getSingleAnnotation(ClassInfo classInfo, DotName annotationName)
			throws AssertionFailure {
		List<AnnotationInstance> annotationList = classInfo.annotations().get( annotationName );
		if ( annotationList == null ) {
			return null;
		}
		else if ( annotationList.size() == 1 ) {
			return annotationList.get( 0 );
		}
		else {
			throw new AssertionFailure(
					"There should be only one annotation of type " + annotationName + " defined on" + classInfo.name()
			);
		}
	}
}


