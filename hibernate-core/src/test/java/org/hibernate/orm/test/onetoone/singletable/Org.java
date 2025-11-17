/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.singletable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Org extends Entity {
	public Set addresses = new HashSet();
}
