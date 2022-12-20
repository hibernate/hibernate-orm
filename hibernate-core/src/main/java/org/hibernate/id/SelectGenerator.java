/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.generator.InDatabaseGenerator;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import static org.hibernate.generator.internal.NaturalIdHelper.getNaturalIdPropertyNames;

/**
 * A generator that {@code select}s the just-{@code insert}ed row to determine the
 * column value assigned by the database. The correct row is located using a unique
 * key of the entity, either:
 * <ul>
 * <li>the mapped {@linkplain org.hibernate.annotations.NaturalId} of the entity, or
 * <li>a property specified using the parameter named {@code "key"}.
 * </ul>
 * The second approach is provided for backward compatibility with older versions of
 * Hibernate.
 * <p>
 * This generator is intended for use with primary keys assigned by a database trigger
 * or something similar, for example:
 * <pre>{@code
 * @Entity @Table(name="TableWithPKAssignedByTrigger")
 * @GenericGenerator(name = "triggered", type = SelectGenerator.class)
 * public class TriggeredEntity {
 *     @Id @GeneratedValue(generator = "triggered")
 *     private Long id;
 *
 *     @NaturalId
 *     private String name;
 *
 *     ...
 * }
 * }</pre>
 * However, after a very long working life, this generator is now handing over its
 * work to {@link org.hibernate.generator.internal.GeneratedGeneration}, and the
 * above code may be written as:
 * <pre>{@code
 * @Entity @Table(name="TableWithPKAssignedByTrigger")
 * public class TriggeredEntity {
 *     @Id @Generated
 *     private Long id;
 *
 *     @NaturalId
 *     private String name;
 *
 *     ...
 * }
 * }</pre>
 * For tables with identity/autoincrement columns, use {@link IdentityGenerator}.
 * <p>
 * The actual work involved in retrieving the primary key value is the job of
 * {@link org.hibernate.id.insert.UniqueKeySelectingDelegate}.
 * <p>
 * Arguably, this class breaks the natural separation of responsibility between the
 * {@linkplain InDatabaseGenerator generator} and the coordinating code, since its
 * role is to specify how the generated value is <em>retrieved</em>.
 *
 * @see org.hibernate.annotations.NaturalId
 * @see org.hibernate.id.insert.UniqueKeySelectingDelegate
 *
 * @author Gavin King
 */
public class SelectGenerator
		implements PostInsertIdentifierGenerator, BulkInsertionCapableIdentifierGenerator, StandardGenerator {
	private String uniqueKeyPropertyName;

	@Override
	public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) {
		uniqueKeyPropertyName = parameters.getProperty( "key" );
	}

	@Override
	public String[] getUniqueKeyPropertyNames(EntityPersister persister) {
		return uniqueKeyPropertyName != null
				? new String[] { uniqueKeyPropertyName }
				: getNaturalIdPropertyNames( persister );
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return false;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return null;
	}
}
