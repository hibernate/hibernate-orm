/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.collectionbasictype;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;

/**
 * @author Vlad Mihalcea
 */
public class CommaDelimitedStringMapJavaType extends AbstractClassJavaType<Map<String, String>> {

	public static final String DELIMITER = ",";

	public CommaDelimitedStringMapJavaType() {
		//noinspection unchecked
		super(
				(Class<? extends Map<String, String>>) (Class<?>) Map.class,
				new MutableMutabilityPlan<>() {
					@Override
					protected Map<String, String> deepCopyNotNull(Map<String, String> value) {
						return new HashMap<>( value );
					}
				}
		);
	}

	@Override
	public String toString(Map<String, String> value) {
		return null;
	}

	@Override
	public Map<String, String> fromString(CharSequence string) {
		return null;
	}

	@Override
	public <X> X unwrap(Map<String, String> value, Class<X> type, WrapperOptions options) {
		return (X) toString( value );
	}

	@Override
	public <X> Map<String, String> wrap(X value, WrapperOptions options) {
		return fromString( (String) value );
	}
}
