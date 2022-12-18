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
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.id.insert.InsertReturningDelegate;
import org.hibernate.id.insert.UniqueKeySelectingDelegate;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

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

	/**
	 * The name of a property of the entity which may be used to locate the just-{@code insert}ed
	 * row containing the generated value. Of course, the columns mapped by this property should
	 * form a unique key of the entity.
	 */
	protected String getUniqueKeyPropertyName(EntityPersister persister) {
		if ( uniqueKeyPropertyName != null ) {
			return uniqueKeyPropertyName;
		}
		int[] naturalIdPropertyIndices = persister.getNaturalIdentifierProperties();
		if ( naturalIdPropertyIndices == null ) {
			throw new IdentifierGenerationException(
					"no natural-id property defined; need to specify [key] in " +
							"generator parameters"
			);
		}
		if ( naturalIdPropertyIndices.length > 1 ) {
			throw new IdentifierGenerationException(
					"select generator does not currently support composite " +
							"natural-id properties; need to specify [key] in generator parameters"
			);
		}
		if ( persister.getEntityMetamodel().isNaturalIdentifierInsertGenerated() ) {
			throw new IdentifierGenerationException(
					"natural-id also defined as insert-generated; need to specify [key] " +
							"in generator parameters"
			);
		}
		return persister.getPropertyNames()[naturalIdPropertyIndices[0]];
	}

	@Override
	public InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(PostInsertIdentityPersister persister) {
		Dialect dialect = persister.getFactory().getJdbcServices().getDialect();
		if ( dialect.supportsInsertReturning() ) {
			//TODO: this is not quite right, since TableInsertReturningBuilder and then TableInsertStandard
			//      ultimately end up calling the SqlAstTranslator to generate the SQL which on H2 delegates
			//      back to IdentityColumnSupport, and this just might not be an identity column
			return new InsertReturningDelegate( persister, dialect );
		}
		else {
			return new UniqueKeySelectingDelegate( persister, dialect, getUniqueKeyPropertyName( persister ) );
		}
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return false;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return new String[0];
	}
}
