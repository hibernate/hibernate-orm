/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.compile;

import java.util.Calendar;
import java.util.Date;
import javax.persistence.Parameter;

/**
 * @author Steve Ebersole
 */
public class ExplicitParameterInfo<T> implements Parameter<T> {
	private final String name;
	private final Integer position;
	private final Class<T> type;

	public ExplicitParameterInfo(String name, Integer position, Class<T> type) {
		if ( name == null && position == null ) {
			throw new IllegalStateException( "Both name and position were null; caller should have generated parameter name" );
		}
		if ( name != null && position != null ) {
			throw new IllegalStateException( "Both name and position were specified" );
		}

		this.name = name;
		this.position = position;
		this.type = type;
	}

	public boolean isNamed() {
		return name != null;
	}

	public String getName() {
		return name;
	}

	public Integer getPosition() {
		return position;
	}

	@Override
	public Class<T> getParameterType() {
		return type;
	}

	/**
	 * Renders this parameter's JPQL form
	 *
	 * @return The rendered form
	 */
	public String render() {
		return isNamed()
				? ":" + name
				: "?" + position.toString();
	}

	public void validateBindValue(Object value) {
		if ( value == null ) {
			return;
		}

		if ( ! getParameterType().isInstance( value ) ) {
			if ( isNamed() ) {
				throw new IllegalArgumentException(
						String.format(
								"Named parameter [%s] type mismatch; expecting [%s] but found [%s]",
								getName(),
								getParameterType().getSimpleName(),
								value.getClass().getSimpleName()
						)
				);
			}
			else {
				throw new IllegalArgumentException(
						String.format(
								"Positional parameter [%s] type mismatch; expecting [%s] but found [%s]",
								getPosition(),
								getParameterType().getSimpleName(),
								value.getClass().getSimpleName()
						)
				);
			}
		}
	}

	public void validateCalendarBind() {
		if ( ! Calendar.class.isAssignableFrom( getParameterType() ) ) {
			if ( isNamed() ) {
				throw new IllegalArgumentException(
						String.format(
								"Named parameter [%s] type mismatch; Calendar was passed, but parameter defined as [%s]",
								getName(),
								getParameterType().getSimpleName()
						)
				);
			}
			else {
				throw new IllegalArgumentException(
						String.format(
								"Positional parameter [%s] type mismatch; Calendar was passed, but parameter defined as [%s]",
								getPosition(),
								getParameterType().getSimpleName()
						)
				);
			}
		}
	}

	public void validateDateBind() {
		if ( !Date.class.isAssignableFrom( getParameterType() ) ) {
			if ( isNamed() ) {
				throw new IllegalArgumentException(
						String.format(
								"Named parameter [%s] type mismatch; Date was passed, but parameter defined as [%s]",
								getName(),
								getParameterType().getSimpleName()
						)
				);
			}
			else {
				throw new IllegalArgumentException(
						String.format(
								"Positional parameter [%s] type mismatch; Date was passed, but parameter defined as [%s]",
								getPosition(),
								getParameterType().getSimpleName()
						)
				);
			}
		}
	}
}
