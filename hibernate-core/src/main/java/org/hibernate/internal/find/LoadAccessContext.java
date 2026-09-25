package org.hibernate.internal.find;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public interface LoadAccessContext {
	SharedSessionContractImplementor getEntityHandler();
}
