/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.boot.models.hbm.type;

import java.net.URL;
import java.util.List;
import java.util.UUID;

/**
 * @author Steve Ebersole
 */
public class EntityWithElementCollections {
	private Integer id;
	private String name;

	private List<String> listOfStrings;
	private List<Integer> listOfIntegers;
	private List<Double> listOfDoubles;
	private List<URL> listOfUrls;
	private List<UUID> listOfUuids;
}
