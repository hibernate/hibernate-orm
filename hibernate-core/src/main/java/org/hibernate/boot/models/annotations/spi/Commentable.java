package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

/**
 * Commonality for annotations which contain SQL comments
 *
 * @author Steve Ebersole
 */
public interface Commentable extends Annotation {
	String comment();

	void comment(String value);
}
