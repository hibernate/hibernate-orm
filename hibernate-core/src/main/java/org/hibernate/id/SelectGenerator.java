/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.internal.NaturalIdHelper.getNaturalIdPropertyNames;

/**
 * A generator that {@code select}s the just-{@code insert}ed row to determine the
 * column value assigned by the database. The correct row is located using a unique
 * key of the entity, either:
 * <ul>
 * <li>the mapped {@linkplain org.hibernate.annotations.NaturalId} of the entity, or
 * <li>a property specified using the parameter named {@value #KEY}.
 * </ul>
 * <p>
 * The second approach is provided for backward compatibility with older versions of
 * Hibernate.
 * <p>
 * This generator is intended for use with primary keys assigned by a database trigger
 * or something similar, for example:
 * <pre>
 * &#64;Entity &#64;Table(name="TableWithPKAssignedByTrigger")
 * &#64;GenericGenerator(name = "triggered", type = SelectGenerator.class)
 * public class TriggeredEntity {
 *     &#64;Id @GeneratedValue(generator = "triggered")
 *     private Long id;
 *
 *     &#64;NaturalId
 *     private String name;
 *
 *     ...
 * }
 * </pre>
 * <p>
 * However, after a very long working life, this generator is now handing over its
 * work to {@link org.hibernate.generator.internal.GeneratedGeneration}, and the
 * above code may be written as:
 * <pre>
 * &#64;Entity &#64;Table(name="TableWithPKAssignedByTrigger")
 * public class TriggeredEntity {
 *     &#64;Id &#64;Generated
 *     private Long id;
 *
 *     &#64;NaturalId
 *     private String name;
 *
 *     ...
 * }
 * </pre>
 * <p>
 * For tables with identity/autoincrement columns, use {@link IdentityGenerator}.
 * <p>
 * The actual work involved in retrieving the primary key value is the job of
 * {@link org.hibernate.id.insert.UniqueKeySelectingDelegate}.
 * <p>
 * Arguably, this class breaks the natural separation of responsibility between the
 * {@linkplain OnExecutionGenerator generator} and the coordinating code, since its
 * role is to specify how the generated value is <em>retrieved</em>.
 *
 * @see org.hibernate.annotations.NaturalId
 * @see org.hibernate.id.insert.UniqueKeySelectingDelegate
 *
 * @author Gavin King
 *
 * @implNote This also implements the {@code select} generation type in {@code hbm.xml} mappings.
 */
public class SelectGenerator
		implements PostInsertIdentifierGenerator, BulkInsertionCapableIdentifierGenerator {

	/**
	 * The property specifying the unique key name.
	 */
	public static final String KEY = "key";

	private String uniqueKeyPropertyName;

	@Override
	public void configure(GeneratorCreationContext creationContext, Properties parameters) {
		uniqueKeyPropertyName = parameters.getProperty( KEY );
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
