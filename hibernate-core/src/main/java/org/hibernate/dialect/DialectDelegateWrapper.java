/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.TemporaryTableExporter;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.loader.ast.spi.MultiKeyLoadSizingStrategy;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.internal.TableMigrator;
import org.hibernate.tool.schema.spi.Cleaner;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * A wrapper of Dialect, to allow decorating some selected methods
 * without having to extend the original class.
 * This is used by Hibernate Reactive, possibly useful for others too,
 * best maintained in the Hibernate ORM core repository to ensure
 * alignment with the Dialect contract.
 */
@Incubating
public class DialectDelegateWrapper extends Dialect {

	protected final Dialect wrapped;

	public DialectDelegateWrapper(Dialect wrapped) {
		this.wrapped = Objects.requireNonNull( wrapped );
	}

	/**
	 * Extract the wrapped dialect, recursively until a non-wrapped implementation is found;
	 * this is useful for all the code needing to know "of which type" the underlying
	 * implementation actually is.
	 * @param dialect the Dialect to unwrap
	 * @return a Dialect implementation which is not a DialectDelegateWrapper; could be the same as the argument.
	 */
	public static Dialect extractRealDialect(Dialect dialect) {
		Objects.requireNonNull( dialect );
		if ( !( dialect instanceof DialectDelegateWrapper ) ) {
			return dialect;
		}
		else {
			return extractRealDialect( ( (DialectDelegateWrapper) dialect ).wrapped );
		}
	}

    /**
	 * Exposed so to allow code needing to know the implementation.
	 * @return the wrapped Dialect
	 */
	public Dialect getWrappedDialect() {
		return wrapped;
	}

	//can't be overridden because of how Dialects get initialized: see constructor of parent
	@Override
	protected final void checkVersion() {
		//intentionally empty: this is used by the super constructor (yuk)
	}

	//can't be overridden because of how Dialects get initialized: see constructor of parent
	@Override
	protected final void registerDefaultKeywords() {
		//intentionally empty: this is used by the super constructor (yuk)
	}

	//can't be overridden because of how Dialects get initialized: see constructor of parent
	@Override
	protected final void initDefaultProperties() {
		//intentionally empty: this is used by the super constructor (yuk)
	}

	@Override
	public void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		wrapped.registerColumnTypes( typeContributions, serviceRegistry );
	}

	@Override
	public String columnType(int sqlTypeCode) {
		return wrapped.columnType( sqlTypeCode );
	}

	@Override
	public String castType(int sqlTypeCode) {
		return wrapped.castType( sqlTypeCode );
	}

	@Override
	public void registerKeywords(DialectResolutionInfo info) {
		wrapped.registerKeywords( info );
	}

	@Override
	public DatabaseVersion getVersion() {
		return wrapped.getVersion();
	}

	@Override
	public DatabaseVersion getMinimumSupportedVersion() {
		return wrapped.getMinimumSupportedVersion();
	}

	@Override
	public Integer resolveSqlTypeCode(String columnTypeName, TypeConfiguration typeConfiguration) {
		return wrapped.resolveSqlTypeCode( columnTypeName, typeConfiguration );
	}

	@Override
	public Integer resolveSqlTypeCode(String typeName, String baseTypeName, TypeConfiguration typeConfiguration) {
		return wrapped.resolveSqlTypeCode( typeName, baseTypeName, typeConfiguration );
	}

	@Override
	public ParameterMarkerStrategy getNativeParameterMarkerStrategy() {
		return wrapped.getNativeParameterMarkerStrategy();
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		return wrapped.resolveSqlTypeDescriptor( columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry );
	}

	@Override
	public int resolveSqlTypeLength(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			int displaySize) {
		return wrapped.resolveSqlTypeLength( columnTypeName, jdbcTypeCode, precision, scale, displaySize );
	}

	@Override
	public String getEnumTypeDeclaration(String name, String[] values) {
		return wrapped.getEnumTypeDeclaration( name, values );
	}

	@Override
	public String getCheckCondition(String columnName, String[] values) {
		return wrapped.getCheckCondition( columnName, values );
	}

	@Override
	public String getCheckCondition(String columnName, long min, long max) {
		return wrapped.getCheckCondition( columnName, min, max );
	}

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		wrapped.contributeFunctions( functionContributions );
	}

	@Override
	public int ordinal() {
		return wrapped.ordinal();
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		wrapped.initializeFunctionRegistry( functionContributions );
	}

	@Override
	public String currentDate() {
		return wrapped.currentDate();
	}

	@Override
	public String currentTime() {
		return wrapped.currentTime();
	}

	@Override
	public String currentTimestamp() {
		return wrapped.currentTimestamp();
	}

	@Override
	public String currentLocalTime() {
		return wrapped.currentLocalTime();
	}

	@Override
	public String currentLocalTimestamp() {
		return wrapped.currentLocalTimestamp();
	}

	@Override
	public String currentTimestampWithTimeZone() {
		return wrapped.currentTimestampWithTimeZone();
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		return wrapped.extractPattern( unit );
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		return wrapped.castPattern( from, to );
	}

	@Override
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		return wrapped.trimPattern( specification, isWhitespace );
	}

	@Override
	public boolean supportsFractionalTimestampArithmetic() {
		return wrapped.supportsFractionalTimestampArithmetic();
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return wrapped.timestampdiffPattern( unit, fromTemporalType, toTemporalType );
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return wrapped.timestampaddPattern( unit, temporalType, intervalType );
	}

	@Override
	public boolean equivalentTypes(int typeCode1, int typeCode2) {
		return wrapped.equivalentTypes( typeCode1, typeCode2 );
	}

	@Override
	public Properties getDefaultProperties() {
		return wrapped.getDefaultProperties();
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return wrapped.getDefaultStatementBatchSize();
	}

	@Override
	public boolean getDefaultNonContextualLobCreation() {
		return wrapped.getDefaultNonContextualLobCreation();
	}

	@Override
	public boolean getDefaultUseGetGeneratedKeys() {
		return wrapped.getDefaultUseGetGeneratedKeys();
	}

	@Override
	public String toString() {
		return wrapped.toString();
	}

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		wrapped.contribute( typeContributions, serviceRegistry );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		wrapped.contributeTypes( typeContributions, serviceRegistry );
	}

	@Override
	public LobMergeStrategy getLobMergeStrategy() {
		return wrapped.getLobMergeStrategy();
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return wrapped.getNativeIdentifierGeneratorStrategy();
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return wrapped.getIdentityColumnSupport();
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return wrapped.getSequenceSupport();
	}

	@Override
	public String getQuerySequencesString() {
		return wrapped.getQuerySequencesString();
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return wrapped.getSequenceInformationExtractor();
	}

	@Override
	public String getSelectGUIDString() {
		return wrapped.getSelectGUIDString();
	}

	@Override
	public boolean supportsTemporaryTables() {
		return wrapped.supportsTemporaryTables();
	}

	@Override
	public boolean supportsTemporaryTablePrimaryKey() {
		return wrapped.supportsTemporaryTablePrimaryKey();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return wrapped.getLimitHandler();
	}

	@Override
	public boolean supportsLockTimeouts() {
		return wrapped.supportsLockTimeouts();
	}

	@Override
	public LockingStrategy getLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		return wrapped.getLockingStrategy( lockable, lockMode );
	}

	@Override
	public String getForUpdateString(LockOptions lockOptions) {
		return wrapped.getForUpdateString( lockOptions );
	}

	@Override
	public String getForUpdateString(LockMode lockMode) {
		return wrapped.getForUpdateString( lockMode );
	}

	@Override
	public String getForUpdateString() {
		return wrapped.getForUpdateString();
	}

	@Override
	public String getWriteLockString(int timeout) {
		return wrapped.getWriteLockString( timeout );
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		return wrapped.getWriteLockString( aliases, timeout );
	}

	@Override
	public String getReadLockString(int timeout) {
		return wrapped.getReadLockString( timeout );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		return wrapped.getReadLockString( aliases, timeout );
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return wrapped.getWriteRowLockStrategy();
	}

	@Override
	public RowLockStrategy getReadRowLockStrategy() {
		return wrapped.getReadRowLockStrategy();
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return wrapped.supportsOuterJoinForUpdate();
	}

	@Override
	public String getForUpdateString(String aliases) {
		return wrapped.getForUpdateString( aliases );
	}

	@Override
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		return wrapped.getForUpdateString( aliases, lockOptions );
	}

	@Override
	public String getForUpdateNowaitString() {
		return wrapped.getForUpdateNowaitString();
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return wrapped.getForUpdateSkipLockedString();
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return wrapped.getForUpdateNowaitString( aliases );
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return wrapped.getForUpdateSkipLockedString( aliases );
	}

	@Override
	public String appendLockHint(LockOptions lockOptions, String tableName) {
		return wrapped.appendLockHint( lockOptions, tableName );
	}

	@Override
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		return wrapped.applyLocksToSql( sql, aliasedLockOptions, keyColumnNames );
	}

	@Override
	public String getCreateTableString() {
		return wrapped.getCreateTableString();
	}

	@Override
	public String getTableTypeString() {
		return wrapped.getTableTypeString();
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return wrapped.supportsIfExistsBeforeTableName();
	}

	@Override
	public boolean supportsIfExistsAfterTableName() {
		return wrapped.supportsIfExistsAfterTableName();
	}

	@Override
	public String getDropTableString(String tableName) {
		return wrapped.getDropTableString( tableName );
	}

	@Override
	public String getCreateIndexString(boolean unique) {
		return wrapped.getCreateIndexString( unique );
	}

	@Override
	public String getCreateIndexTail(boolean unique, List<Column> columns) {
		return wrapped.getCreateIndexTail( unique, columns );
	}

	@Override
	public boolean qualifyIndexName() {
		return wrapped.qualifyIndexName();
	}

	@Override
	public String getCreateMultisetTableString() {
		return wrapped.getCreateMultisetTableString();
	}

	@Override
	public boolean hasAlterTable() {
		return wrapped.hasAlterTable();
	}

	@Override
	public String getAlterTableString(String tableName) {
		return wrapped.getAlterTableString( tableName );
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return wrapped.supportsIfExistsAfterAlterTable();
	}

	@Override
	public String getAddColumnString() {
		return wrapped.getAddColumnString();
	}

	@Override
	public String getAddColumnSuffixString() {
		return wrapped.getAddColumnSuffixString();
	}

	@Override
	public boolean dropConstraints() {
		return wrapped.dropConstraints();
	}

	@Override
	public String getDropForeignKeyString() {
		return wrapped.getDropForeignKeyString();
	}

	@Override
	public String getDropUniqueKeyString() {
		return wrapped.getDropUniqueKeyString();
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return wrapped.supportsIfExistsBeforeConstraintName();
	}

	@Override
	public boolean supportsIfExistsAfterConstraintName() {
		return wrapped.supportsIfExistsAfterConstraintName();
	}

	@Override
	public boolean supportsAlterColumnType() {
		return wrapped.supportsAlterColumnType();
	}

	@Override
	public String getAlterColumnTypeString(String columnName, String columnType, String columnDefinition) {
		return wrapped.getAlterColumnTypeString( columnName, columnType, columnDefinition );
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		return wrapped.getAddForeignKeyConstraintString(
				constraintName,
				foreignKey,
				referencedTable,
				primaryKey,
				referencesPrimaryKey
		);
	}

	@Override
	public String getAddForeignKeyConstraintString(String constraintName, String foreignKeyDefinition) {
		return wrapped.getAddForeignKeyConstraintString( constraintName, foreignKeyDefinition );
	}

	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return wrapped.getAddPrimaryKeyConstraintString( constraintName );
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return wrapped.getFallbackSqmMutationStrategy( entityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return wrapped.getFallbackSqmInsertStrategy( entityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public String getCreateUserDefinedTypeKindString() {
		return wrapped.getCreateUserDefinedTypeKindString();
	}

	@Override
	public String getCreateUserDefinedTypeExtensionsString() {
		return wrapped.getCreateUserDefinedTypeExtensionsString();
	}

	@Override
	public boolean supportsIfExistsBeforeTypeName() {
		return wrapped.supportsIfExistsBeforeTypeName();
	}

	@Override
	public boolean supportsIfExistsAfterTypeName() {
		return wrapped.supportsIfExistsAfterTypeName();
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int position) throws SQLException {
		return wrapped.registerResultSetOutParameter( statement, position );
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, String name) throws SQLException {
		return wrapped.registerResultSetOutParameter( statement, name );
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement) throws SQLException {
		return wrapped.getResultSet( statement );
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
		return wrapped.getResultSet( statement, position );
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException {
		return wrapped.getResultSet( statement, name );
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return wrapped.supportsCurrentTimestampSelection();
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return wrapped.isCurrentTimestampSelectStringCallable();
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return wrapped.getCurrentTimestampSelectString();
	}

	@Override
	public boolean supportsStandardCurrentTimestampFunction() {
		return wrapped.supportsStandardCurrentTimestampFunction();
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return wrapped.buildSQLExceptionConversionDelegate();
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return wrapped.getViolatedConstraintNameExtractor();
	}

	@Override
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		return wrapped.getSelectClauseNullString( sqlType, typeConfiguration );
	}

	@Override
	public boolean supportsUnionAll() {
		return wrapped.supportsUnionAll();
	}

	@Override
	public boolean supportsUnionInSubquery() {
		return wrapped.supportsUnionInSubquery();
	}

	@Override
	@Deprecated(since = "6")
	public String getNoColumnsInsertString() {
		return wrapped.getNoColumnsInsertString();
	}

	@Override
	public boolean supportsNoColumnsInsert() {
		return wrapped.supportsNoColumnsInsert();
	}

	@Override
	public String getLowercaseFunction() {
		return wrapped.getLowercaseFunction();
	}

	@Override
	public String getCaseInsensitiveLike() {
		return wrapped.getCaseInsensitiveLike();
	}

	@Override
	public boolean supportsCaseInsensitiveLike() {
		return wrapped.supportsCaseInsensitiveLike();
	}

	@Override
	public boolean supportsTruncateWithCast() {
		return wrapped.supportsTruncateWithCast();
	}

	@Override
	public String transformSelectString(String select) {
		return wrapped.transformSelectString( select );
	}

	@Override
	public int getMaxAliasLength() {
		return wrapped.getMaxAliasLength();
	}

	@Override
	public int getMaxIdentifierLength() {
		return wrapped.getMaxIdentifierLength();
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return wrapped.toBooleanValueString( bool );
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		wrapped.appendBooleanValueString( appender, bool );
	}

	@Override
	public void registerKeyword(String word) {
		wrapped.registerKeyword( word );
	}

	@Override
	public Set<String> getKeywords() {
		return wrapped.getKeywords();
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {
		return wrapped.buildIdentifierHelper( builder, dbMetaData );
	}

	@Override
	public char openQuote() {
		return wrapped.openQuote();
	}

	@Override
	public char closeQuote() {
		return wrapped.closeQuote();
	}

	@Override
	public String toQuotedIdentifier(String name) {
		return wrapped.toQuotedIdentifier( name );
	}

	@Override
	public String quote(String name) {
		return wrapped.quote( name );
	}

	@Override
	@Incubating
	public SchemaManagementTool getFallbackSchemaManagementTool(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry) {
		return wrapped.getFallbackSchemaManagementTool( configurationValues, registry );
	}

	@Override
	public Exporter<Table> getTableExporter() {
		return wrapped.getTableExporter();
	}

	@Override
	public TableMigrator getTableMigrator() {
		return wrapped.getTableMigrator();
	}

	@Override
	public Cleaner getTableCleaner() {
		return wrapped.getTableCleaner();
	}

	@Override
	public Exporter<UserDefinedType> getUserDefinedTypeExporter() {
		return wrapped.getUserDefinedTypeExporter();
	}

	@Override
	public Exporter<Sequence> getSequenceExporter() {
		return wrapped.getSequenceExporter();
	}

	@Override
	public Exporter<Index> getIndexExporter() {
		return wrapped.getIndexExporter();
	}

	@Override
	public Exporter<ForeignKey> getForeignKeyExporter() {
		return wrapped.getForeignKeyExporter();
	}

	@Override
	public Exporter<Constraint> getUniqueKeyExporter() {
		return wrapped.getUniqueKeyExporter();
	}

	@Override
	public Exporter<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectExporter() {
		return wrapped.getAuxiliaryDatabaseObjectExporter();
	}

	@Override
	public TemporaryTableExporter getTemporaryTableExporter() {
		return wrapped.getTemporaryTableExporter();
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return wrapped.getSupportedTemporaryTableKind();
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return wrapped.getTemporaryTableCreateOptions();
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return wrapped.getTemporaryTableCreateCommand();
	}

	@Override
	public String getTemporaryTableDropCommand() {
		return wrapped.getTemporaryTableDropCommand();
	}

	@Override
	public String getTemporaryTableTruncateCommand() {
		return wrapped.getTemporaryTableTruncateCommand();
	}

	@Override
	public String getCreateTemporaryTableColumnAnnotation(int sqlTypeCode) {
		return wrapped.getCreateTemporaryTableColumnAnnotation( sqlTypeCode );
	}

	@Override
	public TempTableDdlTransactionHandling getTemporaryTableDdlTransactionHandling() {
		return wrapped.getTemporaryTableDdlTransactionHandling();
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return wrapped.getTemporaryTableAfterUseAction();
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return wrapped.getTemporaryTableBeforeUseAction();
	}

	@Override
	public boolean canCreateCatalog() {
		return wrapped.canCreateCatalog();
	}

	@Override
	public String[] getCreateCatalogCommand(String catalogName) {
		return wrapped.getCreateCatalogCommand( catalogName );
	}

	@Override
	public String[] getDropCatalogCommand(String catalogName) {
		return wrapped.getDropCatalogCommand( catalogName );
	}

	@Override
	public boolean canCreateSchema() {
		return wrapped.canCreateSchema();
	}

	@Override
	public String[] getCreateSchemaCommand(String schemaName) {
		return wrapped.getCreateSchemaCommand( schemaName );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		return wrapped.getDropSchemaCommand( schemaName );
	}

	@Override
	public String getCurrentSchemaCommand() {
		return wrapped.getCurrentSchemaCommand();
	}

	@Override
	public SchemaNameResolver getSchemaNameResolver() {
		return wrapped.getSchemaNameResolver();
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return wrapped.hasSelfReferentialForeignKeyBug();
	}

	@Override
	public String getNullColumnString() {
		return wrapped.getNullColumnString();
	}

	@Override
	public String getNullColumnString(String columnType) {
		return wrapped.getNullColumnString( columnType );
	}

	@Override
	public boolean supportsCommentOn() {
		return wrapped.supportsCommentOn();
	}

	@Override
	public String getTableComment(String comment) {
		return wrapped.getTableComment( comment );
	}

	@Override
	public String getUserDefinedTypeComment(String comment) {
		return wrapped.getUserDefinedTypeComment( comment );
	}

	@Override
	public String getColumnComment(String comment) {
		return wrapped.getColumnComment( comment );
	}

	@Override
	public boolean supportsColumnCheck() {
		return wrapped.supportsColumnCheck();
	}

	@Override
	public boolean supportsTableCheck() {
		return wrapped.supportsTableCheck();
	}

	@Override
	public boolean supportsCascadeDelete() {
		return wrapped.supportsCascadeDelete();
	}

	@Override
	public String getCascadeConstraintsString() {
		return wrapped.getCascadeConstraintsString();
	}

	@Override
	public ColumnAliasExtractor getColumnAliasExtractor() {
		return wrapped.getColumnAliasExtractor();
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return wrapped.useInputStreamToInsertBlob();
	}

	@Override
	public boolean useConnectionToCreateLob() {
		return wrapped.useConnectionToCreateLob();
	}

	@Override
	public boolean supportsOrdinalSelectItemReference() {
		return wrapped.supportsOrdinalSelectItemReference();
	}

	@Override
	public NullOrdering getNullOrdering() {
		return wrapped.getNullOrdering();
	}

	@Override
	public boolean supportsNullPrecedence() {
		return wrapped.supportsNullPrecedence();
	}

	@Override
	@Deprecated(since = "6")
	public boolean isAnsiNullOn() {
		return wrapped.isAnsiNullOn();
	}

	@Override
	public boolean requiresCastForConcatenatingNonStrings() {
		return wrapped.requiresCastForConcatenatingNonStrings();
	}

	@Override
	public boolean requiresFloatCastingOfIntegerDivision() {
		return wrapped.requiresFloatCastingOfIntegerDivision();
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return wrapped.supportsResultSetPositionQueryMethodsOnForwardOnlyCursor();
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		return wrapped.supportsCircularCascadeDeleteConstraints();
	}

	@Override
	public boolean supportsSubselectAsInPredicateLHS() {
		return wrapped.supportsSubselectAsInPredicateLHS();
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return wrapped.supportsExpectedLobUsagePattern();
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return wrapped.supportsLobValueChangePropagation();
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return wrapped.supportsUnboundedLobLocatorMaterialization();
	}

	@Override
	public boolean supportsSubqueryOnMutatingTable() {
		return wrapped.supportsSubqueryOnMutatingTable();
	}

	@Override
	public boolean supportsExistsInSelect() {
		return wrapped.supportsExistsInSelect();
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return wrapped.doesReadCommittedCauseWritersToBlockReaders();
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return wrapped.doesRepeatableReadCauseReadersToBlockWriters();
	}

	@Override
	public boolean supportsBindAsCallableArgument() {
		return wrapped.supportsBindAsCallableArgument();
	}

	@Override
	public boolean supportsTupleCounts() {
		return wrapped.supportsTupleCounts();
	}

	@Override
	public boolean requiresParensForTupleCounts() {
		return wrapped.requiresParensForTupleCounts();
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return wrapped.supportsTupleDistinctCounts();
	}

	@Override
	public boolean requiresParensForTupleDistinctCounts() {
		return wrapped.requiresParensForTupleDistinctCounts();
	}

	@Override
	public int getInExpressionCountLimit() {
		return wrapped.getInExpressionCountLimit();
	}

	@Override
	public int getParameterCountLimit() {
		return wrapped.getParameterCountLimit();
	}

	@Override
	public boolean forceLobAsLastValue() {
		return wrapped.forceLobAsLastValue();
	}

	@Override
	public boolean isEmptyStringTreatedAsNull() {
		return wrapped.isEmptyStringTreatedAsNull();
	}

	@Override
	public boolean useFollowOnLocking(String sql, QueryOptions queryOptions) {
		return wrapped.useFollowOnLocking( sql, queryOptions );
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return wrapped.getUniqueDelegate();
	}

	@Override
	public String getQueryHintString(String query, List<String> hintList) {
		return wrapped.getQueryHintString( query, hintList );
	}

	@Override
	public String getQueryHintString(String query, String hints) {
		return wrapped.getQueryHintString( query, hints );
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return wrapped.defaultScrollMode();
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return wrapped.supportsOffsetInSubquery();
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		return wrapped.supportsOrderByInSubquery();
	}

	@Override
	public boolean supportsSubqueryInSelect() {
		return wrapped.supportsSubqueryInSelect();
	}

	@Override
	public boolean supportsInsertReturning() {
		return wrapped.supportsInsertReturning();
	}

	@Override
	public boolean supportsInsertReturningGeneratedKeys() {
		return wrapped.supportsInsertReturningGeneratedKeys();
	}

	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		return wrapped.supportsFetchClause( type );
	}

	@Override
	public boolean supportsWindowFunctions() {
		return wrapped.supportsWindowFunctions();
	}

	@Override
	public boolean supportsLateral() {
		return wrapped.supportsLateral();
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return wrapped.getCallableStatementSupport();
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return wrapped.getNameQualifierSupport();
	}

	@Override
	public MultiKeyLoadSizingStrategy getBatchLoadSizingStrategy() {
		return wrapped.getBatchLoadSizingStrategy();
	}

	@Override
	public MultiKeyLoadSizingStrategy getMultiKeyLoadSizingStrategy() {
		return wrapped.getMultiKeyLoadSizingStrategy();
	}

	@Override
	public boolean isJdbcLogWarningsEnabledByDefault() {
		return wrapped.isJdbcLogWarningsEnabledByDefault();
	}

	@Override
	public void augmentPhysicalTableTypes(List<String> tableTypesList) {
		wrapped.augmentPhysicalTableTypes( tableTypesList );
	}

	@Override
	public void augmentRecognizedTableTypes(List<String> tableTypesList) {
		wrapped.augmentRecognizedTableTypes( tableTypesList );
	}

	@Override
	public boolean supportsPartitionBy() {
		return wrapped.supportsPartitionBy();
	}

	@Override
	public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) throws SQLException {
		return wrapped.supportsNamedParameters( databaseMetaData );
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return wrapped.getNationalizationSupport();
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return wrapped.getAggregateSupport();
	}

	@Override
	public boolean supportsStandardArrays() {
		return wrapped.supportsStandardArrays();
	}

	@Override
	public String getArrayTypeName(String javaElementTypeName, String elementTypeName, Integer maxLength) {
		return wrapped.getArrayTypeName( javaElementTypeName, elementTypeName, maxLength );
	}

	@Override
	public void appendArrayLiteral(
			SqlAppender appender,
			Object[] literal,
			JdbcLiteralFormatter<Object> elementFormatter,
			WrapperOptions wrapperOptions) {
		wrapped.appendArrayLiteral( appender, literal, elementFormatter, wrapperOptions );
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return wrapped.supportsDistinctFromPredicate();
	}

	@Override
	public int getPreferredSqlTypeCodeForArray() {
		return wrapped.getPreferredSqlTypeCodeForArray();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return wrapped.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return wrapped.supportsNonQueryWithCTE();
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return wrapped.supportsRecursiveCTE();
	}

	@Override
	public boolean supportsValuesList() {
		return wrapped.supportsValuesList();
	}

	@Override
	public boolean supportsValuesListForInsert() {
		return wrapped.supportsValuesListForInsert();
	}

	@Override
	public boolean supportsSkipLocked() {
		return wrapped.supportsSkipLocked();
	}

	@Override
	public boolean supportsNoWait() {
		return wrapped.supportsNoWait();
	}

	@Override
	public boolean supportsWait() {
		return wrapped.supportsWait();
	}

	@Override
	public void appendLiteral(SqlAppender appender, String literal) {
		wrapped.appendLiteral( appender, literal );
	}

	@Override
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		wrapped.appendBinaryLiteral( appender, bytes );
	}

	@Override
	public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
		return wrapped.supportsJdbcConnectionLobCreation( databaseMetaData );
	}

	@Override
	public boolean supportsMaterializedLobAccess() {
		return wrapped.supportsMaterializedLobAccess();
	}

	@Override
	public boolean useMaterializedLobWhenCapacityExceeded() {
		return wrapped.useMaterializedLobWhenCapacityExceeded();
	}

	@Override
	public String addSqlHintOrComment(String sql, QueryOptions queryOptions, boolean commentsEnabled) {
		return wrapped.addSqlHintOrComment( sql, queryOptions, commentsEnabled );
	}

	@Override
	public String prependComment(String sql, String comment) {
		return wrapped.prependComment( sql, comment );
	}

	public static String escapeComment(String comment) {
		return Dialect.escapeComment( comment );
	}

	@Override
	public HqlTranslator getHqlTranslator() {
		return wrapped.getHqlTranslator();
	}

	@Override
	public SqmTranslatorFactory getSqmTranslatorFactory() {
		return wrapped.getSqmTranslatorFactory();
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return wrapped.getSqlAstTranslatorFactory();
	}

	@Override
	public SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return wrapped.getGroupBySelectItemReferenceStrategy();
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return wrapped.getSizeStrategy();
	}

	@Override
	public int getMaxVarcharLength() {
		return wrapped.getMaxVarcharLength();
	}

	@Override
	public int getMaxNVarcharLength() {
		return wrapped.getMaxNVarcharLength();
	}

	@Override
	public int getMaxVarbinaryLength() {
		return wrapped.getMaxVarbinaryLength();
	}

	@Override
	public int getMaxVarcharCapacity() {
		return wrapped.getMaxVarcharCapacity();
	}

	@Override
	public int getMaxNVarcharCapacity() {
		return wrapped.getMaxNVarcharCapacity();
	}

	@Override
	public int getMaxVarbinaryCapacity() {
		return wrapped.getMaxVarbinaryCapacity();
	}

	@Override
	public long getDefaultLobLength() {
		return wrapped.getDefaultLobLength();
	}

	@Override
	public int getDefaultDecimalPrecision() {
		return wrapped.getDefaultDecimalPrecision();
	}

	@Override
	public int getDefaultTimestampPrecision() {
		return wrapped.getDefaultTimestampPrecision();
	}

	@Override
	public int getFloatPrecision() {
		return wrapped.getFloatPrecision();
	}

	@Override
	public int getDoublePrecision() {
		return wrapped.getDoublePrecision();
	}

	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return wrapped.getFractionalSecondPrecisionInNanos();
	}

	@Override
	public boolean supportsBitType() {
		return wrapped.supportsBitType();
	}

	@Override
	public boolean supportsPredicateAsExpression() {
		return wrapped.supportsPredicateAsExpression();
	}

	@Override
	public RowLockStrategy getLockRowIdentifier(LockMode lockMode) {
		return wrapped.getLockRowIdentifier( lockMode );
	}

	@Override
	public String generatedAs(String generatedAs) {
		return wrapped.generatedAs( generatedAs );
	}

	@Override
	public boolean hasDataTypeBeforeGeneratedAs() {
		return wrapped.hasDataTypeBeforeGeneratedAs();
	}

	@Override
	public MutationOperation createOptionalTableUpdateOperation(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate optionalTableUpdate,
			SessionFactoryImplementor factory) {
		return wrapped.createOptionalTableUpdateOperation( mutationTarget, optionalTableUpdate, factory );
	}

	@Override
	public boolean canDisableConstraints() {
		return wrapped.canDisableConstraints();
	}

	@Override
	public String getDisableConstraintsStatement() {
		return wrapped.getDisableConstraintsStatement();
	}

	@Override
	public String getEnableConstraintsStatement() {
		return wrapped.getEnableConstraintsStatement();
	}

	@Override
	public String getDisableConstraintStatement(String tableName, String name) {
		return wrapped.getDisableConstraintStatement( tableName, name );
	}

	@Override
	public String getEnableConstraintStatement(String tableName, String name) {
		return wrapped.getEnableConstraintStatement( tableName, name );
	}

	@Override
	public boolean canBatchTruncate() {
		return wrapped.canBatchTruncate();
	}

	@Override
	public String[] getTruncateTableStatements(String[] tableNames) {
		return wrapped.getTruncateTableStatements( tableNames );
	}

	@Override
	public String getTruncateTableStatement(String tableName) {
		return wrapped.getTruncateTableStatement( tableName );
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		wrapped.appendDatetimeFormat( appender, format );
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		return wrapped.translateExtractField( unit );
	}

	@Override
	public String translateDurationField(TemporalUnit unit) {
		return wrapped.translateDurationField( unit );
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		wrapped.appendDateTimeLiteral( appender, temporalAccessor, precision, jdbcTimeZone );
	}

	@Override
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		wrapped.appendDateTimeLiteral( appender, date, precision, jdbcTimeZone );
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Calendar calendar,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		wrapped.appendDateTimeLiteral( appender, calendar, precision, jdbcTimeZone );
	}

	@Override
	public void appendIntervalLiteral(SqlAppender appender, Duration literal) {
		wrapped.appendIntervalLiteral( appender, literal );
	}

	@Override
	public void appendUUIDLiteral(SqlAppender appender, UUID literal) {
		wrapped.appendUUIDLiteral( appender, literal );
	}

	@Override
	public boolean supportsTemporalLiteralOffset() {
		return wrapped.supportsTemporalLiteralOffset();
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return wrapped.getTimeZoneSupport();
	}

	@Override
	public String rowId(String rowId) {
		return wrapped.rowId( rowId );
	}

	@Override
	public int rowIdSqlType() {
		return wrapped.rowIdSqlType();
	}

	@Override
	public String getRowIdColumnString(String rowId) {
		return wrapped.getRowIdColumnString( rowId );
	}
}
