/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
