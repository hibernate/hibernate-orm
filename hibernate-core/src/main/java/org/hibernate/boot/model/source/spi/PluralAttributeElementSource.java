package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
@Remove
public interface PluralAttributeElementSource {
	PluralAttributeElementNature getNature();
}
