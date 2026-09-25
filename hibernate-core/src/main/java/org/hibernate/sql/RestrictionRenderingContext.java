package org.hibernate.sql;

import org.hibernate.Internal;

/**
 * @author Steve Ebersole
 */
@Internal
public interface RestrictionRenderingContext {
	String makeParameterMarker();
}
