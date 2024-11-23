/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;
import java.util.HashMap;
import java.util.Map;

public class Company {
	int id;
	Map organization = new HashMap();
	Map conferenceRoomExtensions = new HashMap();
}
