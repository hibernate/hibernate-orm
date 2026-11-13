package org.hibernate.usertype;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

import org.hibernate.HibernateException;

/**
 * A custom type that may function as an identifier or discriminator type
 *
 * @author Gavin King
 */
@SPI({ USE, IMPLEMENT })
public interface EnhancedUserType<J> extends UserType<J> {

	/**
	 * Return an SQL literal representation of the value
	 */
	@Nonnull
	String toSqlLiteral(@Nonnull J value);

	/**
	 * Render the value to the string representation.
	 *
	 * @param value The value to render to string.
	 *
	 * @return The string representation
	 *
	 * @throws HibernateException Problem rendering
	 */
	@Nullable
	String toString(@Nonnull J value) throws HibernateException;

	/**
	 * Consume the given string representation back into this types java form.
	 *
	 * @param sequence The string representation to be consumed.
	 *
	 * @return The java type representation
	 *
	 * @throws HibernateException Problem consuming
	 */
	@Nullable
	J fromStringValue(@Nonnull CharSequence sequence) throws HibernateException;

}
