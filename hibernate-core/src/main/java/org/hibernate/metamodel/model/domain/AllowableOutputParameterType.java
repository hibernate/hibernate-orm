/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

/**
 * Specialization of DomainType for types that can be used as a
 * parameter output for a {@link org.hibernate.procedure.ProcedureCall}
 *
 * @apiNote Generally speaking, an allowable output parameter type
 * can only effectively be a basic type.
 *
 * @author Steve Ebersole
 */
public interface AllowableOutputParameterType<J> extends AllowableParameterType<J> {
}
