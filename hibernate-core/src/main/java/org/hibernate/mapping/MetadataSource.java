/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;


/**
 * Enumeration of the known places from which a piece of metadata may come.
 *
 * @author Steve Ebersole
 */
public enum MetadataSource {
	HBM,
	ANNOTATIONS,
	OTHER
}
