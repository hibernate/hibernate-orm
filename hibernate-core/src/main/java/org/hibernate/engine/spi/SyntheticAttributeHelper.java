/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.spi;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class SyntheticAttributeHelper {
	public static final String SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME = "_identifierMapper";

	private static final String BACKREF_ATTRIBUTE_NAME_PREFIX = "_";
	private static final String BACKREF_ATTRIBUTE_NAME_SUFFIX = "BackRef";
	private static final String INDEX_BACKREF_ATTRIBUTE_NAME_SUFFIX = "IndexBackRef";


	public static String createBackRefAttributeName(String pluralAttributeRole) {
		return createSyntheticAttributeName(
				pluralAttributeRole,
				BACKREF_ATTRIBUTE_NAME_PREFIX,
				BACKREF_ATTRIBUTE_NAME_SUFFIX
		);
	}

	public static String createIndexBackRefAttributeName(String pluralAttributeRole) {
		return createSyntheticAttributeName(
				pluralAttributeRole,
				BACKREF_ATTRIBUTE_NAME_PREFIX,
				INDEX_BACKREF_ATTRIBUTE_NAME_SUFFIX
		);
	}

	private static String createSyntheticAttributeName(String attributeRole, String prefix, String suffix) {
		return new StringBuilder(
				prefix.length() + attributeRole.length() + suffix.length() )
				.append( prefix )
				.append( attributeRole )
				.append( suffix )
				.toString();
	}

}
