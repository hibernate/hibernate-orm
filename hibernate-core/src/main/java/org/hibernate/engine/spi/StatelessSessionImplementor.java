package org.hibernate.engine.spi;

import org.hibernate.StatelessSession;

/**
 * SPI extension of StatelessSession
 *
 * @author Steve Ebersole
 *
 * @since 7.2
 */
public interface StatelessSessionImplementor extends StatelessSession, SharedSessionContractImplementor {
	@Override
	default boolean isStateless() {
		return true;
	}
}
