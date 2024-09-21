/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.collectionbasictype;

import java.sql.Types;
import java.util.List;

import org.hibernate.usertype.UserTypeSupport;

/**
 * @author Vlad Mihalcea
 */
public class CommaDelimitedStringsType extends UserTypeSupport<List<String>> {
	public CommaDelimitedStringsType() {
		super( List.class, Types.VARCHAR );
	}
}
