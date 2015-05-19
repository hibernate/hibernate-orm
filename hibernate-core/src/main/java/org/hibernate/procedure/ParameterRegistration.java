/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure;

import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;

import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public interface ParameterRegistration<T> {
	/**
	 * The name under which this parameter was registered.  Can be {@code null} which should indicate that
	 * positional registration was used (and therefore {@link #getPosition()} should return non-null.
	 *
	 * @return The name;
	 */
	public String getName();

	/**
	 * The position at which this parameter was registered.  Can be {@code null} which should indicate that
	 * named registration was used (and therefore {@link #getName()} should return non-null.
	 *
	 * @return The name;
	 */
	public Integer getPosition();

	/**
	 * Obtain the Java type of parameter.  This is used to guess the Hibernate type (unless {@link #setHibernateType}
	 * is called explicitly).
	 *
	 * @return The parameter Java type.
	 */
	public Class<T> getType();

	/**
	 * Retrieves the parameter "mode" which describes how the parameter is defined in the actual database procedure
	 * definition (is it an INPUT parameter?  An OUTPUT parameter? etc).
	 *
	 * @return The parameter mode.
	 */
	public ParameterMode getMode();

	/**
	 * Set the Hibernate mapping type for this parameter.
	 *
	 * @param type The Hibernate mapping type.
	 */
	public void setHibernateType(Type type);

	/**
	 * Retrieve the binding associated with this parameter.  The binding is only relevant for INPUT parameters.  Can
	 * return {@code null} if nothing has been bound yet.  To bind a value to the parameter use one of the
	 * {@link #bindValue} methods.
	 *
	 * @return The parameter binding
	 */
	public ParameterBind<T> getBind();

	/**
	 * Bind a value to the parameter.  How this value is bound to the underlying JDBC CallableStatement is
	 * totally dependent on the Hibernate type.
	 *
	 * @param value The value to bind.
	 */
	public void bindValue(T value);

	/**
	 * Bind a value to the parameter, using just a specified portion of the DATE/TIME value.  It is illegal to call
	 * this form if the parameter is not DATE/TIME type.  The Hibernate type is circumvented in this case and
	 * an appropriate "precision" Type is used instead.
	 *
	 * @param value The value to bind
	 * @param explicitTemporalType An explicitly supplied TemporalType.
	 */
	public void bindValue(T value, TemporalType explicitTemporalType);
}
