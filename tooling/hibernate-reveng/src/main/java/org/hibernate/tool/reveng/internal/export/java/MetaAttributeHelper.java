/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.java;

import org.hibernate.mapping.MetaAttribute;

import java.util.Collection;
import java.util.Iterator;

/**
 * Helper for loading, merging  and accessing <meta> tags.
 *
 * @author max
 *
 *
 */
public final class MetaAttributeHelper {

	private MetaAttributeHelper() {
		//noop
	}

	/**
	 * @param meta
	 * @param separator
	 */
	public static String getMetaAsString(Collection<?> meta, String separator) {
		if(meta==null || meta.isEmpty() ) {
			return "";
		}
		StringBuffer buf = new StringBuffer();

			for (Iterator<?> iter = meta.iterator(); iter.hasNext();) {
				buf.append(iter.next() );
				if(iter.hasNext() ) buf.append(separator);
			}
		return buf.toString();
	}

	public static String getMetaAsString(MetaAttribute meta, String seperator) {
		if(meta==null) {
			return null;
		}
		else {
			return getMetaAsString(meta.getValues(),seperator);
		}
	}

	static	boolean getMetaAsBool(Collection<?> c, boolean defaultValue) {
			if(c==null || c.isEmpty() ) {
				return defaultValue;
			}
			else {
				return Boolean.valueOf(c.iterator().next().toString() ).booleanValue();
			}
		}

	public static String getMetaAsString(MetaAttribute c) {
		return c==null?"":getMetaAsString(c.getValues() );
	}

	static String getMetaAsString(Collection<?> c) {
		return getMetaAsString(c, "");
	}

	public static boolean getMetaAsBool(MetaAttribute metaAttribute, boolean defaultValue) {
		return getMetaAsBool(metaAttribute==null?null:metaAttribute.getValues(), defaultValue);
	}



}
