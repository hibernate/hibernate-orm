/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.jpa.boot.spi.Bootstrap;

import org.jipijapa.plugin.spi.EntityManagerFactoryBuilder;

/**
 * TwoPhaseBootstrapImpl
 *
 * @author Scott Marlow
 */
public class TwoPhaseBootstrapImpl implements EntityManagerFactoryBuilder {

	private final org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder entityManagerFactoryBuilder;

	@SuppressWarnings("WeakerAccess")
	public TwoPhaseBootstrapImpl(final PersistenceUnitInfo info, final Map map) {
		entityManagerFactoryBuilder =
				Bootstrap.getEntityManagerFactoryBuilder( info, map );
	}

	@Override
	public EntityManagerFactory build() {
		return entityManagerFactoryBuilder.build();
	}

	@Override
	public void cancel() {
		entityManagerFactoryBuilder.cancel();
	}

	@Override
	public EntityManagerFactoryBuilder withValidatorFactory(Object validatorFactory) {
		entityManagerFactoryBuilder.withValidatorFactory( validatorFactory );
		return this;
	}

}
