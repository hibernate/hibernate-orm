/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.internal.ImplicitNamingContextImpl;
import org.hibernate.boot.model.naming.internal.ImplicitNamingHelper;
import org.hibernate.boot.model.naming.spi.ArrayNamingInput;
import org.hibernate.boot.model.naming.spi.EnumNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.ImplicitNamingDefaults;
import org.hibernate.boot.model.naming.spi.JavaTypeNamingInput;
import org.hibernate.boot.model.naming.spi.NamedSqlTypeKind;
import org.hibernate.boot.model.naming.spi.NamedSqlTypeNamingInput;
import org.hibernate.boot.model.naming.spi.NamingNamePair;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;

import static org.hibernate.boot.model.naming.internal.PhysicalNamingStrategyHelper.physicalIdentifier;

/// Boot-scoped naming decisions retained across finalized metadata restoration.
/// Keys contain immutable value snapshots, not mappings, services, or Optional instances.
///
/// @author Steve Ebersole
public final class NamedSqlTypeNames implements Serializable {
	private final Map<List<String>, LogicalName> logicalNames = new HashMap<>();
	private final Map<String, LogicalName> enumNames = new HashMap<>();
	private final Map<String, LogicalName> arrayNames = new HashMap<>();
	private transient Database database;
	private transient ImplicitNamingStrategy strategy;
	private transient ImplicitNamingContext context;
	private transient LogicalName defaultCatalog;
	private transient LogicalName defaultSchema;

	public void attach(Database database, MappingResolutionOptions options) {
		this.database = database;
		strategy = options.getImplicitNamingStrategy();
		final var defaults = options.getMappingDefaults();
		defaultCatalog = logical( Identifier.toIdentifier( defaults.getImplicitCatalogName() ) );
		defaultSchema = logical( Identifier.toIdentifier( defaults.getImplicitSchemaName() ) );
		context = new ImplicitNamingContextImpl( new ImplicitNamingDefaults() {
			@Override public boolean isDefaultQuoteIdentifiers() { return defaults.shouldImplicitlyQuoteIdentifiers(); }
			@Override public String getDefaultIdColumnName() { return defaults.getImplicitIdColumnName(); }
			@Override public String getDefaultDiscriminatorColumnName() { return defaults.getImplicitDiscriminatorColumnName(); }
			@Override public String getDefaultTenantIdColumnName() { return defaults.getImplicitTenantIdColumnName(); }
		}, database.getJdbcEnvironment().getIdentifierHelper(), options.getSchemaCharset() );
	}

	public static JavaTypeNamingInput javaType(Class<?> type) {
		return new JavaTypeNamingInput( type.getName(), type.getSimpleName(),
				type.isArray() ? Optional.of( javaType( type.getComponentType() ) ) : Optional.empty() );
	}

	public static Optional<JavaTypeNamingInput> converter(BasicValueConverter<?, ?> converter) {
		return converter == null ? Optional.empty() : Optional.of( javaType(
				converter instanceof JpaAttributeConverter<?, ?> jpa
						? jpa.getConverterJavaType().getJavaTypeClass() : converter.getClass() ) );
	}

	public String resolveEnum(EnumNamingInput input) {
		final var key = new ArrayList<String>();
		key.add( "enum" );
		addJavaType( key, input.javaType() );
		key.add( input.representation().name() );
		addJavaType( key, input.converterType().orElse( null ) );
		key.addAll( input.values() );
		final var logical = implicit( key, () -> strategy.determineEnumName( input, context ),
				"determineEnumName for " + input.javaType().qualifiedName() );
		final var name = render( database.getDefaultNamespace().resolvePhysicalEnumName( identifier( logical ) ) );
		enumNames.put( name, logical );
		return name;
	}

	public LogicalName resolveArray(ArrayNamingInput input) {
		final var key = new ArrayList<String>();
		key.add( "array" );
		addJavaType( key, input.elementJavaType() );
		addJavaType( key, input.preferredJdbcJavaType().orElse( null ) );
		addJavaType( key, input.converterType().orElse( null ) );
		key.add( Integer.toString( input.elementJdbcTypeCode() ) );
		key.add( input.representation().name() );
		addLogical( key, input.declaredElementTypeName().orElse( null ) );
		if ( input.namedElement().isPresent() ) {
			final var element = input.namedElement().get();
			key.add( element.kind().name() );
			addPair( key, element.name() );
			addPair( key, element.catalog().orElse( null ) );
			addPair( key, element.schema().orElse( null ) );
		}
		return implicit( key, () -> strategy.determineArrayName( input, context ),
				"determineArrayName for " + input.elementJavaType().qualifiedName() );
	}

	public String physicalArray(LogicalName logical) {
		final var name = render( database.getDefaultNamespace().resolvePhysicalArrayName( identifier( logical ) ) );
		arrayNames.put( name, logical );
		return name;
	}

	public NamedSqlTypeNamingInput dependency(NamedSqlTypeKind kind, String physicalName, LogicalName declared) {
		final var qualified = QualifiedNameParser.INSTANCE.parse( physicalName );
		final var object = qualified.getObjectName();
		final var logical = declared != null ? declared
				: (kind == NamedSqlTypeKind.ENUM ? enumNames : arrayNames).get( physicalName );
		final var namespace = database.getDefaultNamespace();
		return new NamedSqlTypeNamingInput( kind,
				pair( logical == null ? new LogicalName( object.getText(), object.isQuoted(), true ) : logical, object ),
				qualifier( namespace.getName().catalog() == null ? defaultCatalog : namespace.getName().catalog(), qualified.getCatalogName() ),
				qualifier( namespace.getName().schema() == null ? defaultSchema : namespace.getName().schema(), qualified.getSchemaName() ) );
	}

	public NamedSqlTypeNamingInput structDependency(String declaredName) {
		final var qualified = QualifiedNameParser.INSTANCE.parse( declaredName );
		final var catalog = logical( qualified.getCatalogName() );
		final var schema = logical( qualified.getSchemaName() );
		final var namespace = database.locateNamespace( catalog, schema );
		final var logical = logical( qualified.getObjectName() );
		final var physical = namespace.resolvePhysicalStructName( qualified.getObjectName() );
		return new NamedSqlTypeNamingInput( NamedSqlTypeKind.STRUCT, pair( logical, physical ),
				qualifier( catalog, physicalIdentifier( namespace.getPhysicalName().catalog() ) ),
				qualifier( schema, physicalIdentifier( namespace.getPhysicalName().schema() ) ) );
	}

	private static LogicalName logical(Identifier name) {
		return name == null ? null : new LogicalName( name.getText(), name.isQuoted(), true );
	}

	private Optional<NamingNamePair> qualifier(LogicalName logical, Identifier physical) {
		return physical == null ? Optional.empty() : Optional.of( pair(
				logical == null ? new LogicalName( physical.getText(), physical.isQuoted(), true ) : logical, physical ) );
	}

	private NamingNamePair pair(LogicalName logical, Identifier physical) {
		return new NamingNamePair( logical, database.getJdbcEnvironment().getIdentifierHelper()
				.getPhysicalNameFactory().create( physical.getText(), physical.isQuoted() ) );
	}

	private LogicalName implicit(List<String> key, Supplier<LogicalName> action, String role) {
		return logicalNames.computeIfAbsent( key, ignored -> {
			final var name = action.get();
			ImplicitNamingHelper.columnName( name, role );
			return name;
		} );
	}

	private static Identifier identifier(LogicalName name) {
		return Identifier.toIdentifier( name.getText(), name.isQuoted(), false, name.isExplicit() );
	}

	private static void addJavaType(List<String> key, JavaTypeNamingInput type) {
		key.add( type == null ? null : type.qualifiedName() );
		if ( type != null ) {
			key.add( type.simpleName() );
			addJavaType( key, type.arrayComponent().orElse( null ) );
		}
	}

	private static void addLogical(List<String> key, LogicalName name) {
		key.add( name == null ? null : name.getText() );
		if ( name != null ) {
			key.add( Boolean.toString( name.isQuoted() ) );
			key.add( Boolean.toString( name.isExplicit() ) );
		}
	}

	private static void addPair(List<String> key, NamingNamePair pair) {
		addLogical( key, pair == null ? null : pair.logicalName() );
		if ( pair != null ) {
			key.add( pair.physicalName().getText() );
			key.add( Boolean.toString( pair.physicalName().isQuoted() ) );
		}
	}

	private String render(Identifier name) {
		final var qualifiers = database.getDefaultNamespace().getPhysicalName();
		return SqlStringGenerationContextImpl.fromConfigurationMap( database.getJdbcEnvironment(), database,
				database.getServiceRegistry().requireService( ConfigurationService.class ).getSettings() )
				.format( new QualifiedNameImpl(
						physicalIdentifier( qualifiers.catalog() ),
						physicalIdentifier( qualifiers.schema() ), name ) );
	}
}
