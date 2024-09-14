/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.Internal;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.NullJdbcType;

/**
 * A type that is assignable to every non-primitive type,
 * that is, the type of {@code null}. Note that this not
 * a true bottom type, since {@code null} cannot be assigned
 * to primitive types.
 * <p>
 * Since the Java language does not have an explicit bottom
 * type, we follow the type system developed for the Ceylon
 * language and treat {@link Void} as the Java bottom type.
 *
 * @author Gavin King
 */
@Internal
public class BottomType extends AbstractSingleColumnStandardBasicType<Void> {

	public static final BottomType INSTANCE = new BottomType();

	private BottomType() {
		super( NullJdbcType.INSTANCE, new AbstractClassJavaType<>(Void.class) {
			@Override
			public <X> X unwrap(Void value, Class<X> type, WrapperOptions options) {
				return null;
			}
			@Override
			public <X> Void wrap(X value, WrapperOptions options) {
				return null;
			}
		} );
	}

	@Override
	public String getName() {
		return "NULL";
	}
}
