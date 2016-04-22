/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor;

/**
 * Defines the context for {@link WrapperOptions}
 *
 * @author Steve Ebersole
 *
 * @deprecated (since 5.2) Just directly implement WrapperOptions
 */
@Deprecated
public interface WrapperOptionsContext extends WrapperOptions {
	/**
	 * Obtain the WrapperOptions for this context.
	 *
	 * @return The WrapperOptions
	 *
	 * @deprecated (since 5.2) Just directly implement WrapperOptions
	 */
	@Deprecated
	default WrapperOptions getWrapperOptions() {
		return this;
	}
}
