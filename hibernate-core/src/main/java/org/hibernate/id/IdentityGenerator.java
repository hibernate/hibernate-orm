/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A generator for use with ANSI-SQL IDENTITY columns used as the primary key.
 * The IdentityGenerator for autoincrement/identity key generation.
 * <p>
 * Indicates to the {@code Session} that identity (i.e. identity/autoincrement
 * column) key generation should be used.
 *
 * @implNote Most of the functionality of this generator is delegated to
 * 		     {@link InsertGeneratedIdentifierDelegate}.
 *
 * @author Christoph Sturm
 */
public class IdentityGenerator
		implements PostInsertIdentifierGenerator, BulkInsertionCapableIdentifierGenerator, StandardGenerator {
}
