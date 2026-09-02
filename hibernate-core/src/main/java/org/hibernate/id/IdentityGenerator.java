/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.insert.AppendingIdentitySelectDelegate;
import org.hibernate.id.insert.BasicSelectingDelegate;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.id.insert.InsertReturningDelegate;
import org.hibernate.id.insert.UniqueKeySelectingDelegate;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.internal.NaturalIdHelper.getNaturalIdPropertyNames;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.noCustomSql;

/**
 * An {@link OnExecutionGenerator} that handles {@code IDENTITY}/"autoincrement"
 * columns on those databases which support them.
 * <p>
 * Delegates to the {@link org.hibernate.dialect.identity.spi.IdentityColumnSupport}
 * provided by the {@linkplain Dialect#getIdentityColumnSupport() dialect}.
 * <p>
 * The actual work involved in retrieving the primary key value is the job of a
 * {@link org.hibernate.generator.values.GeneratedValuesMutationDelegate}.
 *
 * @see jakarta.persistence.GenerationType#IDENTITY
 * @see org.hibernate.dialect.identity.spi.IdentityColumnSupport
 * @see org.hibernate.generator.values.GeneratedValuesMutationDelegate
 *
 * @author Christoph Sturm
 *
 * @implNote This also implements the {@code identity} generation type in {@code hbm.xml} mappings.
 */
public class IdentityGenerator
		implements PostInsertIdentifierGenerator, BulkInsertionCapableIdentifierGenerator {
	private Class<?> generatedType;

	@Override
	public void configure(GeneratorCreationContext creationContext, Properties parameters) {
		generatedType = creationContext.getType().getReturnedClass();
	}

	@Override
	public Class<?> getGeneratedType() {
		return generatedType;
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return dialect.getIdentityColumnSupport().hasIdentityInsertKeyword();
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return new String[] { dialect.getIdentityColumnSupport().getIdentityInsertString() };
	}

	@Override
	@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
	public InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(EntityPersister persister) {
		final var dialect = persister.getFactory().getJdbcServices().getDialect();
		final var sessionFactoryOptions = persister.getFactory().getSessionFactoryOptions();
		final var generatedValuesSupport = dialect.getGeneratedValuesSupport();
		final var generatedProperties = persister.getGeneratedProperties( INSERT );
		if ( generatedProperties.size() == 1 && sessionFactoryOptions.isGetGeneratedKeysEnabled() ) {
			// Use Connection#prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) when only retrieving identity
			assert generatedProperties.get( 0 ).isEntityIdentifierMapping();
			return buildIdentityGeneratedKeysDelegate( persister, dialect );
		}
		// Try to use generic delegates if the dialects supports them
		else if ( generatedValuesSupport.supports( GeneratedValuesSupport.Capability.ARBITRARY_GENERATED_KEYS )
				&& sessionFactoryOptions.isGetGeneratedKeysEnabled() ) {
			return new GetGeneratedKeysDelegate( persister, false, INSERT );
		}
		else if ( generatedValuesSupport.supports( GeneratedValuesSupport.Capability.INSERT_RETURNING )
				&& noCustomSql( persister, INSERT ) ) {
			return new InsertReturningDelegate( persister, INSERT );
		}
		// Fall back to delegates which only handle identifiers
		else if ( sessionFactoryOptions.isGetGeneratedKeysEnabled() ) {
			return buildIdentityGeneratedKeysDelegate( persister, dialect );
		}
		else if ( persister.getNaturalIdentifierProperties() != null
				&& !persister.isNaturalIdentifierInsertGenerated() ) {
			return new UniqueKeySelectingDelegate( persister, getNaturalIdPropertyNames( persister ), INSERT );
		}
		else {
			return new BasicSelectingDelegate( persister );
		}
	}

	private static InsertGeneratedIdentifierDelegate buildIdentityGeneratedKeysDelegate(
			EntityPersister persister,
			Dialect dialect) {
		return switch ( dialect.getIdentityColumnSupport().getIdentityValueRetrieval() ) {
			case INFERRED_GENERATED_KEYS -> new GetGeneratedKeysDelegate( persister, true, INSERT );
			case NAMED_GENERATED_KEYS -> new GetGeneratedKeysDelegate( persister, false, INSERT );
			case APPENDED_SELECT -> new AppendingIdentitySelectDelegate( persister );
		};
	}

	@Override
	public boolean requiresIdentityColumn() {
		return true;
	}
}
