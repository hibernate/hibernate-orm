/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure;

import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.query.QueryParameter;

/**
 * Describes a registered procedure/function parameter.
 * <p/>
 * Conceptually this contract groups together the notion of a
 * {@link QueryParameter} and, depending on that parameter's mode
 * ({@link QueryParameter#getMode()}), the related binding (for IN/INOUT
 * parameters).
 *
 * @author Steve Ebersole
 */
public interface ParameterRegistration<T> extends QueryParameter<T> {
	/**
	 * Controls how unbound values for this IN/INOUT parameter registration will be handled prior to
	 * execution.  There are 2 possible options to handle it:<ul>
	 *     <li>bind the NULL to the parameter</li>
	 *     <li>do not bind the NULL to the parameter</li>
	 * </ul>
	 * <p/>
	 * The reason for the distinction comes from default values defined on the corresponding
	 * database procedure/function argument.  Any time a value (including NULL) is bound to the
	 * argument, its default value will not be used.  So effectively this setting controls
	 * whether the NULL should be interpreted as "pass the NULL" or as "apply the argument default".
	 * <p/>
	 * The (global) default this setting is defined by {@link SessionFactoryOptions#isProcedureParameterNullPassingEnabled()}
	 *
	 * @param enabled {@code true} indicates that the NULL should be passed; {@code false} indicates it should not.
	 */
	void enablePassingNulls(boolean enabled);

	/**
	 * Set the Hibernate Type associated with this parameter.  Affects
	 * the return from {@link #getHibernateType()}.
	 *
	 * @param type The Hibernate Type.
	 */
	void setHibernateType(Type type);

	/**
	 * Retrieve the binding associated with this parameter.  The binding is only relevant for INPUT parameters.  Can
	 * return {@code null} if nothing has been bound yet.  To bind a value to the parameter use one of the
	 * {@link #bindValue} methods.
	 *
	 * @return The parameter binding
	 */
	ParameterBind<T> getBind();

	/**
	 * Bind a value to the parameter.  How this value is bound to the underlying JDBC CallableStatement is
	 * totally dependent on the Hibernate type.
	 *
	 * @param value The value to bind.
	 */
	void bindValue(T value);

	/**
	 * Bind a value to the parameter, using just a specified portion of the DATE/TIME value.  It is illegal to call
	 * this form if the parameter is not DATE/TIME type.  The Hibernate type is circumvented in this case and
	 * an appropriate "precision" Type is used instead.
	 *
	 * @param value The value to bind
	 * @param explicitTemporalType An explicitly supplied TemporalType.
	 */
	void bindValue(T value, TemporalType explicitTemporalType);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * @deprecated (since 6.0) Use {@link #getParameterType()} instead as
	 * ParameterRegistration now extends {@link QueryParameter}.
	 */
	@Deprecated
	default Class<T> getType() {
		return getParameterType();
	}
}
