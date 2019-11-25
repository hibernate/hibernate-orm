/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.compositeid;

import static org.hibernate.cfg.AvailableSettings.USE_GET_GENERATED_KEYS;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.identity.CompositeGeneratedIdentifierSelectingDelegate;
import org.hibernate.dialect.identity.IdentityColumnSupport;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;

/**
 * Same as {@link CompositeIdIdentityTest},
 * setting {@link org.hibernate.cfg.AvailableSettings#USE_GET_GENERATED_KEYS} to false.
 * <p>
 * Allows to tests {@link CompositeGeneratedIdentifierSelectingDelegate},
 * whenever the dialect does not support {@link IdentityColumnSupport#getIdentityInsertString()}.
 *
 * @author Fabio Massimo Ercoli
 */
@RequiresDialectFeature(DialectChecks.SupportsCompositeNestedIdentityColumns.class)
@TestForIssue(jiraKey = "HHH-9662")
public class CompositeIdSelectingIdentityTest extends CompositeIdIdentityTest {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( USE_GET_GENERATED_KEYS, "false" );
	}
}
