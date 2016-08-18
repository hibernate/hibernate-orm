/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.association.inherited;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.hibernate.test.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.testing.TestForIssue;

/**
 * @author Luis Barreiro
 */
public class InheritedAttributeAssociationTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Author.class, Item.class, ChildItem.class };
	}

	@Override
	public void prepare() {
	}

	@Override
	@TestForIssue( jiraKey = "HHH-11050")
	public void execute() {
		// The mapping is wrong but the point is that the enhancement phase does not need to fail. See JIRA for further detail

		// If enhancement of 'items' attribute fails, 'name' won't be enhanced
		Author author = new Author();
		author.name = "Bernardo Soares";
		EnhancerTestUtils.checkDirtyTracking( author, "name" );
	}

	@Override
	protected void cleanup() {
	}

}
