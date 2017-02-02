/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.beanvalidation;

import org.hibernate.event.service.spi.DuplicationStrategy;

/**
 * @author Steve Ebersole
 */
public class DuplicationStrategyImpl implements DuplicationStrategy {
	public static final DuplicationStrategyImpl INSTANCE = new DuplicationStrategyImpl();

	@Override
	public boolean areMatch(Object listener, Object original) {
		return listener.getClass().equals( original.getClass() ) &&
				BeanValidationEventListener.class.equals( listener.getClass() );
	}

	@Override
	public Action getAction() {
		return Action.KEEP_ORIGINAL;
	}
}
