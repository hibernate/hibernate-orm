/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.format.FormatMapper;

public class JsonAggregateMutabilityPlan<T> extends AggregateMutabilityPlan<T> {
	@SuppressWarnings("rawtypes")
	private static final JsonAggregateMutabilityPlan INSTANCE = new JsonAggregateMutabilityPlan();

	private JsonAggregateMutabilityPlan() {
		super();
	}

	@Override
	protected FormatMapper getFormatMapper(WrapperOptions options) {
		return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper();
	}

	public static <X> JsonAggregateMutabilityPlan<X> getInstance() {
		//noinspection unchecked
		return (JsonAggregateMutabilityPlan<X>) INSTANCE;
	}
}
