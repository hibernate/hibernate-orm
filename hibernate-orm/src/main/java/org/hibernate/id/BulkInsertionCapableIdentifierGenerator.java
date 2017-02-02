/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.dialect.Dialect;

/**
 * Specialized contract for {@link IdentifierGenerator} implementations capable of being used in conjunction
 * with HQL insert statements.
 *
 * @author Steve Ebersole
 */
public interface BulkInsertionCapableIdentifierGenerator extends IdentifierGenerator {
	/**
	 * Given the configuration of this generator, is identifier generation as part of bulk insertion supported?
	 * <p/>
	 * IMPL NOTE : Mainly here to allow stuff like SequenceStyleGenerator which *can* support this based on
	 * configuration
	 *
	 * @return {@code true} if bulk insertions are supported; {@code false} otherwise.
	 */
	public boolean supportsBulkInsertionIdentifierGeneration();

	/**
	 * Return the select expression fragment, if any, that generates the identifier values.
	 *
	 * @param dialect The dialect against which the insert will be performed.
	 *
	 * @return The identifier value generation fragment (SQL).  {@code null} indicates that no fragment is needed.
	 */
	public String determineBulkInsertionIdentifierGenerationSelectFragment(Dialect dialect);
}
