/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Uses a unique key of the inserted entity to locate the newly inserted row.
 */
public class UniqueKeySelectingDelegate extends AbstractSelectingDelegate {
	private final PostInsertIdentityPersister persister;
	private final Dialect dialect;

	private final String uniqueKeyPropertyName;
	private final Type uniqueKeyType;
	private final BasicType<?> idType;

	private final String idSelectString;

	public UniqueKeySelectingDelegate(PostInsertIdentityPersister persister, Dialect dialect, String uniqueKeyPropertyName) {
		super( persister );

		this.persister = persister;
		this.dialect = dialect;
		this.uniqueKeyPropertyName = uniqueKeyPropertyName;

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
	public TableInsertBuilder createTableInsertBuilder(
			BasicEntityIdentifierMapping identifierMapping,
			Expectation expectation,
			SessionFactoryImplementor factory) {
		return new TableInsertBuilderStandard( persister, persister.getIdentifierTableMapping(), factory );
	}

	protected void bindParameters(Object entity, PreparedStatement ps, SharedSessionContractImplementor session)
			throws SQLException {
		uniqueKeyType.nullSafeSet( ps, persister.getPropertyValue( entity, uniqueKeyPropertyName ), 1, session );
	}

	@Override
	protected Object extractGeneratedValue(Object entity, ResultSet resultSet, SharedSessionContractImplementor session)
			throws SQLException {
		if ( !resultSet.next() ) {
			throw new IdentifierGenerationException("the inserted row could not be located by the unique key: "
					+ uniqueKeyPropertyName);
		}
		return idType.getJdbcValueExtractor().extract( resultSet, 1, session );
	}
}
