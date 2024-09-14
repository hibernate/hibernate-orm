/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;


import org.hibernate.type.BasicTypeReference;

/**
 * Represents a typed argument to a query parameter.
 * <p>
 * Usually, the {@linkplain org.hibernate.type.Type Hibernate type} of
 * an argument to a query parameter may be inferred, and so it's rarely
 * necessary to explicitly pass a type when binding the argument.
 * Occasionally, and especially when the argument is null, the type
 * cannot be inferred and must be explicitly specified. In such cases,
 * an instance of {@code TypedParameterValue} may be passed to
 * {@link jakarta.persistence.Query#setParameter setParameter()}.
 * <p>
 * For example:
 * <pre>
 * query.setParameter("stringNamedParam",
 *         new TypedParameterValue(StandardBasicTypes.STRING, null))
 * </pre>
 * <p>
 * Here, a "null string" argument was bound to the named parameter
 * {@code :stringNamedParam}.
 *
 * @author Steve Ebersole
 *
 * @since 6
 *
 * @see jakarta.persistence.Query#setParameter(int, Object)
 * @see jakarta.persistence.Query#setParameter(String, Object)
 * @see CommonQueryContract#setParameter(int, Object)
 * @see CommonQueryContract#setParameter(String, Object)
 *
 * @see org.hibernate.type.StandardBasicTypes
 */
public final class TypedParameterValue<J> {

	private final BindableType<J> type;
	private final J value;

	public TypedParameterValue(BindableType<J> type, J value) {
		this.type = type;
		this.value = value;
	}

	public TypedParameterValue(BasicTypeReference<J> type, J value) {
		this.type = type;
		this.value = value;
	}

	/**
	 * The value to bind
	 *
	 * @return The value to be bound
	 */
	public J getValue() {
		return value;
	}

	/**
	 * The specific Hibernate type to use to bind the value.
	 *
	 * @return The Hibernate type to use.
	 */
	public BindableType<J> getType() {
		return type;
	}

	/**
	 * The specific Hibernate type reference to use to bind the value.
	 *
	 * @return The Hibernate type reference to use.
	 */
	public BasicTypeReference<J> getTypeReference() {
		return type instanceof BasicTypeReference ? (BasicTypeReference<J>) type : null;
	}
}
