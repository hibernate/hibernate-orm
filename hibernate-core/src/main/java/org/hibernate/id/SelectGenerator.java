/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.insert.AbstractSelectingDelegate;
import org.hibernate.id.insert.IdentifierGeneratingInsert;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

/**
 * A generator that selects the just inserted row to determine the identifier
 * value assigned by the database. The correct row is located using a unique
 * key.
 * <p>
 * One mapping parameter is required: key (unless a natural-id is defined in the mapping).
 *
 * @author Gavin King
 */
public class SelectGenerator extends AbstractPostInsertGenerator {
	private String uniqueKeyPropertyName;

	@Override
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		uniqueKeyPropertyName = params.getProperty( "key" );
	}

	@Override
	public InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(
			PostInsertIdentityPersister persister,
			Dialect dialect,
			boolean isGetGeneratedKeysEnabled) throws HibernateException {
		return new SelectGeneratorDelegate( persister, dialect, uniqueKeyPropertyName );
	}

	@Internal
	public static String determineNameOfPropertyToUse(PostInsertIdentityPersister persister, String supplied) {
		if ( supplied != null ) {
			return supplied;
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

	private static class SelectGeneratorDelegate extends AbstractSelectingDelegate {
		private final PostInsertIdentityPersister persister;
		private final Dialect dialect;

		private final String uniqueKeyPropertyName;
		private final Type uniqueKeyType;
		private final BasicType<?> idType;

		private final String idSelectString;

		public SelectGeneratorDelegate(
				PostInsertIdentityPersister persister,
				Dialect dialect,
				String configuredPropertyName) {
			super( persister );

			this.persister = persister;
			this.dialect = dialect;
			this.uniqueKeyPropertyName = determineNameOfPropertyToUse( persister, configuredPropertyName );

			idSelectString = persister.getSelectByUniqueKeyString( uniqueKeyPropertyName );
			uniqueKeyType = persister.getPropertyType( uniqueKeyPropertyName );
			idType = (BasicType<?>) persister.getIdentifierType();
		}

		protected String getSelectSQL() {
			return idSelectString;
		}

		@Override
		public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert(SqlStringGenerationContext context) {
			return new IdentifierGeneratingInsert( dialect );
		}

		@Override
		public TableInsertBuilder createTableInsertBuilder(BasicEntityIdentifierMapping identifierMapping, Expectation expectation, SessionFactoryImplementor sessionFactory) {
			return new TableInsertBuilderStandard(
					persister,
					persister.getIdentifierTableMapping(),
					sessionFactory
			);
		}

		protected void bindParameters(
				Object entity, PreparedStatement ps, SharedSessionContractImplementor session) throws SQLException {
			Object uniqueKeyValue = persister.getPropertyValue( entity, uniqueKeyPropertyName );
			uniqueKeyType.nullSafeSet( ps, uniqueKeyValue, 1, session );
		}

		@Override
		protected Object extractGeneratedValue(
				Object entity,
				ResultSet rs,
				SharedSessionContractImplementor session) throws SQLException {
			if ( !rs.next() ) {
				throw new IdentifierGenerationException(
						"the inserted row could not be located by the unique key: " +
								uniqueKeyPropertyName
				);
			}

			return idType.getJdbcValueExtractor().extract(
					rs,
					1,
					session
			);
		}
	}
}
