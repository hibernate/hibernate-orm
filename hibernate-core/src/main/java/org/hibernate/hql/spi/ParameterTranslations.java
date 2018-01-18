/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi;

import java.util.Map;

/**
 * Defines available information about the parameters encountered during
 * query translation.
 *
 * @author Steve Ebersole
 */
public interface ParameterTranslations {
	Map<String,NamedParameterInformation> getNamedParameterInformationMap();
	Map<Integer,PositionalParameterInformation> getPositionalParameterInformationMap();

	PositionalParameterInformation getPositionalParameterInformation(int position);

	NamedParameterInformation getNamedParameterInformation(String name);
}
