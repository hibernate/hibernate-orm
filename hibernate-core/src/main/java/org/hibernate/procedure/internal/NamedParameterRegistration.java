/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import javax.persistence.ParameterMode;

import org.hibernate.type.Type;

/**
 * Represents a registered named parameter
 *
 * @author Steve Ebersole
 */
public class NamedParameterRegistration<T> extends AbstractParameterRegistrationImpl<T> {
	NamedParameterRegistration(
			ProcedureCallImpl procedureCall,
			String name,
			ParameterMode mode,
			Class<T> type) {
		super( procedureCall, name, mode, type );
	}

	NamedParameterRegistration(
			ProcedureCallImpl procedureCall,
			String name,
			ParameterMode mode,
			Class<T> type,
			Type hibernateType) {
		super( procedureCall, name, mode, type, hibernateType );
	}
}
