/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$

package org.hibernate.test.annotations.lob;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

/**
 * @author Emmanuel Bernard
 */
@RequiresDialectFeature(DialectChecks.SupportsExpectedLobUsagePattern.class)
public class LobTest extends AbstractLobTest<Book, CompiledCode> {
	@Override
	protected Class<Book> getBookClass() {
		return Book.class;
	}

	@Override
	protected Integer getId(Book book) {
		return book.getId();
	}

	@Override
	protected Class<CompiledCode> getCompiledCodeClass() {
		return CompiledCode.class;
	}

	@Override
	protected Integer getId(CompiledCode compiledCode) {
		return compiledCode.getId();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				CompiledCode.class
		};
	}
}
