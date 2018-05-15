/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.insert.AbstractSelectingDelegate;
import org.hibernate.id.insert.IdentifierGeneratingInsert;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor;
import org.hibernate.metamodel.valuegen.ValueGenerationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * A generator that selects the just inserted row to determine the identifier
 * value assigned by the database. The correct row is located using a unique
 * key.
 * <p/>
 * One mapping parameter is required: key (unless a natural-id is defined in the mapping).
 *
 * @author Gavin King
 */
public class SelectGenerator extends AbstractPostInsertGenerator implements Configurable {
	private String uniqueKeyPropertyName;

	@Override
	public void configure(JavaTypeDescriptor javaTypeDescriptor, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		uniqueKeyPropertyName = params.getProperty( "key" );
	}

	public InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(
			PostInsertIdentityPersister persister,
			Dialect dialect,
			boolean isGetGeneratedKeysEnabled) throws HibernateException {
		return new SelectGeneratorDelegate( persister, dialect, uniqueKeyPropertyName );
	}

	private static String determineNameOfPropertyToUse(PostInsertIdentityPersister persister, String supplied) {
		if ( supplied != null ) {
			return supplied;
		}

		final NaturalIdDescriptor<?> naturalIdDescriptor = persister.getHierarchy().getNaturalIdDescriptor();
		if ( naturalIdDescriptor == null ) {
			throw new IdentifierGenerationException(
					"no natural-id property defined; need to specify [key] in " +
							"generator parameters"
			);
		}

		if ( naturalIdDescriptor.getAttributeInfos().size() > 1 ) {
			throw new IdentifierGenerationException(
					"select generator does not currently support composite " +
							"natural-id properties; need to specify [key] in generator parameters"
			);
		}

		final ValueGenerationStrategy generationStrategy = naturalIdDescriptor.getAttributeInfos()
				.get( 0 )
				.getUnderlyingAttributeDescriptor()
				.getValueGenerationStrategy();
		if ( generationStrategy.getGenerationTiming() != GenerationTiming.NEVER ) {
			throw new IdentifierGenerationException(
					"natural-id also defined as insert-generated; need to specify [key] " +
							"in generator parameters"
			);
		}

		return naturalIdDescriptor.getAttributeInfos().get( 0 ).getUnderlyingAttributeDescriptor().getAttributeName();
	}


	/**
	 * The delegate for the select generation strategy.
	 */
	public static class SelectGeneratorDelegate
			extends AbstractSelectingDelegate
			implements InsertGeneratedIdentifierDelegate {
		private final PostInsertIdentityPersister persister;
		private final Dialect dialect;

		private final String uniqueKeyPropertyName;
		private final Type uniqueKeyType;
		private final Type idType;

		private final String idSelectString;

		private SelectGeneratorDelegate(
				PostInsertIdentityPersister persister,
				Dialect dialect,
				String suppliedUniqueKeyPropertyName) {
			super( persister );
			this.persister = persister;
			this.dialect = dialect;
			this.uniqueKeyPropertyName = determineNameOfPropertyToUse( persister, suppliedUniqueKeyPropertyName );

			idSelectString = persister.getSelectByUniqueKeyString( uniqueKeyPropertyName );
			uniqueKeyType = persister.getPropertyType( uniqueKeyPropertyName );
			idType = persister.getIdentifierType();
		}

		public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert() {
			return new IdentifierGeneratingInsert( dialect );
		}


		// AbstractSelectingDelegate impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		protected String getSelectSQL() {
			return idSelectString;
		}

		protected void bindParameters(
				SharedSessionContractImplementor session,
				PreparedStatement ps,
				Object entity) throws SQLException {
			throw new NotYetImplementedFor6Exception();
//			Object uniqueKeyValue = persister.getPropertyValue( entity, uniqueKeyPropertyName );
//			uniqueKeyType.nullSafeSet( ps, uniqueKeyValue, 1, session );
		}

		protected Serializable getResult(
				SharedSessionContractImplementor session,
				ResultSet rs,
				Object entity) throws SQLException {
			throw new NotYetImplementedFor6Exception();
//			if ( !rs.next() ) {
//				throw new IdentifierGenerationException(
//						"the inserted row could not be located by the unique key: " +
//								uniqueKeyPropertyName
//				);
//			}
//			return (Serializable) idType.nullSafeGet(
//					rs,
//					persister.getRootTableKeyColumnNames(),
//					session,
//					entity
//			);
		}
	}
}
