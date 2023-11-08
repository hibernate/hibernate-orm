/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.format.FormatMapper;

public class XmlAggregateMutabilityPlan<T> extends AggregateMutabilityPlan<T> {
	@SuppressWarnings("rawtypes")
	private static final XmlAggregateMutabilityPlan INSTANCE = new XmlAggregateMutabilityPlan();

	private XmlAggregateMutabilityPlan() {
		super();
	}

	@Override
	protected FormatMapper getFormatMapper(WrapperOptions options) {
		return options.getSessionFactory().getFastSessionServices().getXmlFormatMapper();
	}

	public static <X> XmlAggregateMutabilityPlan<X> getInstance() {
		//noinspection unchecked
		return (XmlAggregateMutabilityPlan<X>) INSTANCE;
	}
}
