/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
