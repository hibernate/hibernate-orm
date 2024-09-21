/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.collectionbasictype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;

/**
 * @author Vlad Mihalcea
 */
public class CommaDelimitedStringsJavaType extends AbstractClassJavaType<List> {

	public static final String DELIMITER = ",";

	public CommaDelimitedStringsJavaType() {
		super(
			List.class,
			new MutableMutabilityPlan<List>() {
				@Override
				protected List deepCopyNotNull(List value) {
					return new ArrayList( value );
				}
			}
		);
	}

	@Override
	public String toString(List value) {
		return ( (List<String>) value ).stream().collect( Collectors.joining( DELIMITER ) );
	}

	@Override
	public List fromString(CharSequence string) {
		List<String> values = new ArrayList<>();
		Collections.addAll( values, string.toString().split( DELIMITER ) );
		return values;
	}

	@Override
	public <X> X unwrap(List value, Class<X> type, WrapperOptions options) {
		return (X) toString( value );
	}

	@Override
	public <X> List wrap(X value, WrapperOptions options) {
		return fromString( (String) value );
	}
}
