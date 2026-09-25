package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * @author Steve Ebersole
 */
@Remove
public interface FetchCharacteristicsSingularAssociation extends FetchCharacteristics {
	boolean isUnwrapProxies();
}
