/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.uniqueconstraint;

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
