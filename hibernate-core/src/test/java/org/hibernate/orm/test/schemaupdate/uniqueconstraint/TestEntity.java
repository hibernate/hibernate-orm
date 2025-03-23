/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.uniqueconstraint;

import java.util.List;
import java.util.Set;

/**
 * @author Andrea Boriero
 */
public class TestEntity {
	private Long id;

	private List<TestEntity> children;

	private Set<String> items;
}
