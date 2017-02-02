/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder.Options;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Functionality common to stateless and stateful sessions
 *
 * @author Gavin King
 */
public abstract class AbstractSessionImpl
		extends AbstractSharedSessionContract
		implements Serializable, SharedSessionContractImplementor, JdbcSessionOwner, SessionImplementor, EventSource,
		Options, WrapperOptions {

	protected AbstractSessionImpl(SessionFactoryImpl factory, SessionCreationOptions options) {
		super( factory, options );
	}

}
