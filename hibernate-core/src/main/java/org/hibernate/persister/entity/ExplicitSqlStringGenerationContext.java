package org.hibernate.persister.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.relational.naming.spi.QualifiedPhysicalName;

/**
 * SqlStringGenerationContext implementation with support for overriding the
 * default catalog and schema
 *
 * @author Steve Ebersole
 */
public class ExplicitSqlStringGenerationContext implements SqlStringGenerationContext {
	private final JdbcServices jdbcServices;
	private final Identifier defaultCatalog;
	@Nullable
	private final Identifier defaultSchema;

	public ExplicitSqlStringGenerationContext(
			String defaultCatalog,
			String defaultSchema,
			SessionFactoryImplementor factory) {
		this(
				defaultCatalog,
				defaultSchema,
				factory.getSqlStringGenerationContext(),
				factory.getJdbcServices()
		);
	}

	public ExplicitSqlStringGenerationContext(
			String defaultCatalog,
			String defaultSchema,
			SqlStringGenerationContext baseContext,
			JdbcServices jdbcServices) {
		this.jdbcServices = jdbcServices;
		this.defaultCatalog = defaultCatalog != null
				? toIdentifier( defaultCatalog )
				: baseContext.getDefaultCatalog();
		this.defaultSchema = defaultSchema != null
				? toIdentifier( defaultSchema )
				: baseContext.getDefaultSchema();
	}

	@Nonnull
	private JdbcEnvironment getJdbcEnvironment() {
		return jdbcServices.getJdbcEnvironment();
	}

	@Nonnull
	@Override
	public PhysicalName.Factory getPhysicalNameFactory() {
		return getJdbcEnvironment().getIdentifierHelper().getPhysicalNameFactory();
	}

	@Override
	public Dialect getDialect() {
		return jdbcServices.getDialect();
	}

	@Nullable
	@Override
	public Identifier toIdentifier(@Nullable String text) {
		return getJdbcEnvironment().getIdentifierHelper().toIdentifier( text );
	}

	@Nullable
	@Override
	public Identifier getDefaultCatalog() {
		return defaultCatalog;
	}

	@Nullable
	@Override
	public Identifier getDefaultSchema() {
		return defaultSchema;
	}

	@Nonnull
	@Override
	public String format(@Nonnull QualifiedTableName qualifiedName) {
		return nameFormater().format( withDefaults( qualifiedName ), getDialect() );
	}

	@Nonnull
	private QualifiedObjectNameFormatter nameFormater() {
		//noinspection deprecation
		return getJdbcEnvironment().getQualifiedObjectNameFormatter();
	}

	@Nonnull
	@Override
	public String format(@Nonnull QualifiedSequenceName qualifiedName) {
		return nameFormater().format( withDefaults( qualifiedName ), getDialect() );
	}

	@Nonnull
	@Override
	public String format(@Nonnull QualifiedName qualifiedName) {
		return nameFormater().format( withDefaults( qualifiedName ), getDialect() );
	}

	@Nonnull
	@Override
	public String formatWithoutCatalog(@Nonnull QualifiedSequenceName qualifiedName) {
		return nameFormater().format( nameToFormat( qualifiedName ), getDialect() );
	}

	@Nonnull
	private QualifiedSequenceName nameToFormat(@Nonnull QualifiedSequenceName qualifiedName) {
		if ( qualifiedName.getCatalogName() != null
				|| qualifiedName.getSchemaName() == null && defaultSchema != null ) {
			return new QualifiedSequenceName(
					null,
					schemaWithDefault( qualifiedName.getSchemaName() ),
					qualifiedName.getSequenceName()
			);
		}
		else {
			return qualifiedName;
		}
	}

	@Override
	public String format(QualifiedPhysicalName name) {
		return nameFormater().format( physicalNameWithDefaults( name, false ), getDialect() );
	}

	@Override
	public String formatWithoutCatalog(QualifiedPhysicalName name) {
		return nameFormater().format( physicalNameWithDefaults( name, true ), getDialect() );
	}

	private QualifiedPhysicalName physicalNameWithDefaults(QualifiedPhysicalName name, boolean omitCatalog) {
		final var factory = getJdbcEnvironment().getIdentifierHelper().getPhysicalNameFactory();
		return new QualifiedPhysicalName(
				omitCatalog ? null : name.catalogName() != null ? name.catalogName()
						: defaultCatalog == null ? null : factory.create( defaultCatalog.getText(), defaultCatalog.isQuoted() ),
				name.schemaName() != null ? name.schemaName()
						: defaultSchema == null ? null : factory.create( defaultSchema.getText(), defaultSchema.isQuoted() ),
				name.objectName() );
	}

	@Override
	public boolean isMigration() {
		return false;
	}
}
