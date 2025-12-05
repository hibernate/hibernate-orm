/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.tools;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;

import java.util.Locale;

/**
 * Tools for dealing with Hibernate ORM, especially in regard to handling differences
 * between {@linkplain org.hibernate.Session stateful} and {@linkplain StatelessSession stateless}
 * sessions.
 *
 * @author Steve Ebersole
 */
public class OrmTools {
	public static Object loadAuditEntity(String entityName, Object id, SharedSessionContractImplementor session) {
		if ( session instanceof SessionImplementor statefulSession ) {
			return statefulSession.getReference( entityName, id );
		}
		else if ( session instanceof StatelessSession statelessSession ) {
			return statelessSession.get( entityName, id );
		}
		else {
			throw unexpectedSessionType( session );
		}
	}

	public static void saveData(
			String auditEntityName,
			Object data,
			SharedSessionContractImplementor session) {
		if ( session instanceof SessionImplementor statefulSession ) {
			statefulSession.persist( auditEntityName, data );
		}
		else if ( session instanceof StatelessSession statelessSession ) {
			statelessSession.insert( auditEntityName, data );
		}
		else {
			throw unexpectedSessionType( session );
		}
	}

	public static void removeData(Object data, SharedSessionContractImplementor session) {
		if ( session instanceof SessionImplementor statefulSession ) {
			statefulSession.remove( data );
		}
		else if ( session instanceof StatelessSessionImplementor statelessSession ) {
			statelessSession.delete( data );
		}
		else {
			unexpectedSessionType( session );
		}
	}

	private static UnsupportedOperationException unexpectedSessionType(SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( String.format(
				Locale.ROOT,
				"Unexpected argument type (`%s`); expecting `%s` or `%s`",
				session.getClass().getName(),
				Session.class.getName(),
				StatelessSession.class.getName()
		) );

	}
}
