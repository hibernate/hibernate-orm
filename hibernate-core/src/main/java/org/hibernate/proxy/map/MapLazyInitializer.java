/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy.map;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.AbstractLazyInitializer;

/**
 * Lazy initializer for "dynamic-map" entity representations.
 *
 * @author Gavin King
 */
public class MapLazyInitializer extends AbstractLazyInitializer implements Serializable {

	MapLazyInitializer(String entityName, Serializable id, SharedSessionContractImplementor session) {
		super( entityName, id, session );
	}

	public Map getMap() {
		return (Map) getImplementation();
	}

	public Class getPersistentClass() {
		throw new UnsupportedOperationException("dynamic-map entity representation");
	}

	// Expose the following methods to MapProxy by overriding them (so that classes in this package see them)

	@Override
	protected void prepareForPossibleLoadingOutsideTransaction() {
		super.prepareForPossibleLoadingOutsideTransaction();
	}

	@Override
	protected boolean isAllowLoadOutsideTransaction() {
		return super.isAllowLoadOutsideTransaction();
	}

	@Override
	protected String getSessionFactoryUuid() {
		return super.getSessionFactoryUuid();
	}
}
