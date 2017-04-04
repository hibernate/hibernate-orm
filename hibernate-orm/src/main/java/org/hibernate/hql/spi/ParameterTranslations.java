/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi;
import java.util.Set;

import org.hibernate.type.Type;

/**
 * Defines available information about the parameters encountered during
 * query translation.
 *
 * @author Steve Ebersole
 */
public interface ParameterTranslations {

	public boolean supportsOrdinalParameterMetadata();

	public int getOrdinalParameterCount();

	public int getOrdinalParameterSqlLocation(int ordinalPosition);

	public Type getOrdinalParameterExpectedType(int ordinalPosition);

	public Set getNamedParameterNames();

	public int[] getNamedParameterSqlLocations(String name);

	public Type getNamedParameterExpectedType(String name);
}
