/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.id.insert.BasicSelectingDelegate;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.id.insert.InsertReturningDelegate;
import org.hibernate.id.insert.UniqueKeySelectingDelegate;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.internal.NaturalIdHelper.getNaturalIdPropertyNames;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.noCustomSql;

/**
 * An {@link OnExecutionGenerator} that handles {@code IDENTITY}/"autoincrement"
 * columns on those databases which support them.
 * <p>
 * Delegates to the {@link org.hibernate.dialect.identity.IdentityColumnSupport}
 * provided by the {@linkplain Dialect#getIdentityColumnSupport() dialect}.
 * <p>
 * The actual work involved in retrieving the primary key value is the job of a
 * {@link org.hibernate.generator.values.GeneratedValuesMutationDelegate}.
 *
 * @see jakarta.persistence.GenerationType#IDENTITY
 * @see org.hibernate.dialect.identity.IdentityColumnSupport
 * @see org.hibernate.generator.values.GeneratedValuesMutationDelegate
 *
 * @author Christoph Sturm
 *
 * @implNote This also implements the {@code identity} generation type in {@code hbm.xml} mappings.
 */
public class IdentityGenerator
		implements PostInsertIdentifierGenerator, BulkInsertionCapableIdentifierGenerator, StandardGenerator {

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return dialect.getIdentityColumnSupport().hasIdentityInsertKeyword();
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return new String[] { dialect.getIdentityColumnSupport().getIdentityInsertString() };
	}

	@Override
	public InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(PostInsertIdentityPersister persister) {
		final SessionFactoryImplementor factory = persister.getFactory();
		final Dialect dialect = factory.getJdbcServices().getDialect();
		// Try to use generic delegates if the dialects supports them
		final SessionFactoryOptions sessionFactoryOptions = factory.getSessionFactoryOptions();
		if ( dialect.supportsInsertReturningGeneratedKeys() && sessionFactoryOptions.isGetGeneratedKeysEnabled() ) {
			return new GetGeneratedKeysDelegate( persister, false, INSERT );
		}
		else if ( dialect.supportsInsertReturning() && noCustomSql( persister, INSERT ) ) {
			return new InsertReturningDelegate( persister, INSERT );
		}
		// Fall back to delegates which only handle identifiers
		else if ( sessionFactoryOptions.isGetGeneratedKeysEnabled() ) {
			return dialect.getIdentityColumnSupport().buildGetGeneratedKeysDelegate( persister, dialect );
		}
		else if ( persister.getNaturalIdentifierProperties() != null
				&& !persister.getEntityMetamodel().isNaturalIdentifierInsertGenerated() ) {
			return new UniqueKeySelectingDelegate( persister, getNaturalIdPropertyNames( persister ), INSERT );
		}
		else {
			return new BasicSelectingDelegate( persister );
		}
	}
}
