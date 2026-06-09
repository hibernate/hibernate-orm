/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.internal.GeneratorBinder;
import org.hibernate.boot.model.internal.GeneratorStrategies;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.mapping.internal.binders.BasicValueBinder;
import org.hibernate.boot.mapping.internal.binders.ColumnBinder;
import org.hibernate.boot.mapping.internal.binders.ComponentMemberTarget;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.model.ComponentMemberBinding;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.view.AttributeBindingView;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.engine.spi.Managed;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.Basic;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.annotation.Nonnull;

import static jakarta.persistence.GenerationType.AUTO;
import static org.hibernate.boot.mapping.internal.binders.AttributeBinder.applyChecks;
import static org.hibernate.boot.mapping.internal.binders.AttributeBinder.applyColumnTransformer;
import static org.hibernate.boot.mapping.internal.binders.AttributeBinder.processSelectable;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/// Materializes legacy `BasicValue` mapping objects for simple basic-shaped
/// attributes.
///
/// The methods intentionally mirror the existing binder-specific behavior
/// rather than normalizing all basic values into one path.  Normal attributes,
/// version attributes, and tenant-id attributes currently apply different
/// secondary side effects around the same mapping object type.
///
/// @since 9.0
/// @author Steve Ebersole
public class BasicValueMappingMaterializer {
	public BasicValue createAttributeBasicValue(
			AttributeBindingView attributeBinding,
			Property property,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final MemberDetails member = attributeBinding.member();
		validateNonIdentifierGeneratedValue( member, member.getDeclaringType().getClassName() + "." + member.getName() );
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext() );

		final var selectable = processSelectable( attributeBinding, property, basicValue, primaryTable, bindingOptions, bindingState, bindingContext );
		final var column = selectable.column();
		applyBasicOptionality( member, attributeBinding.resolvedType(), property, column );
		applyBasicFetch( member, property );
		property.setLob( member.hasDirectAnnotationUsage( Lob.class ) );

		BasicValueBinder.bindBasicValue(
				BasicValueSource.attribute(
						member,
						attributeBinding.resolvedType(),
						bindingContext
				),
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);

		new AttributeOptionsMappingMaterializer().materializeBasicValueOptions( attributeBinding, basicValue );
		applyFinalFieldMutability( member, property, basicValue, false );

		return basicValue;
	}

	public void materializeVersionBasicValue(
			MemberDetails member,
			BasicValueIntent basicValueIntent,
			Property property,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext(), primaryTable );
		basicValue.makeVersion();
		property.setValue( basicValue );

		BasicValueBinder.bindBasicValue(
				BasicValueSource.attribute( member, bindingContext ),
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);

		final Column column = processSelectable( basicValueIntent, property, basicValue, primaryTable, bindingOptions, bindingState, bindingContext )
				.requireColumn( property.getName() );
		column.setNullable( false );
	}

	public void materializeTenantIdBasicValue(
			MemberDetails member,
			BasicValueIntent basicValueIntent,
			Property property,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext(), primaryTable );
		property.setValue( basicValue );

		processSelectable( basicValueIntent, property, basicValue, primaryTable, bindingOptions, bindingState, bindingContext );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.attribute( member, bindingContext ),
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	public MaterializedBasicValue createComponentMemberBasicValue(
			ComponentSource source,
			ComponentMemberBinding componentMember,
			Property property,
			PersistentClass ownerBinding,
			ComponentMemberTarget memberTarget,
			List<String> columnNamingPatterns,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final MemberDetails member = componentMember.member();
		if ( source.kind() != ComponentSource.Kind.EMBEDDED_IDENTIFIER ) {
			validateNonIdentifierGeneratedValue( member, member.getName() );
		}
		final BasicValueIntent basicValueIntent = componentMember.basicValueIntent();
		final Table valueTable = resolveTable( basicValueIntent, ownerBinding, memberTarget );
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext(), valueTable );
		basicValue.setTable( valueTable );
		property.setValue( basicValue );

		if ( basicValueIntent.isFormula() ) {
			basicValue.addFormula( formula( basicValueIntent ) );
			property.setOptional( true );
			property.setInsertable( false );
			property.setUpdatable( false );
			BasicValueBinder.bindBasicValue(
					BasicValueSource.embeddableMember( member, basicValueIntent.conversion() ),
					property,
					basicValue,
					bindingOptions,
					bindingState,
					bindingContext
			);
			return new MaterializedBasicValue( basicValue, null );
		}

		final Column column = bindComponentMemberColumn(
				() -> implicitBasicColumnName( source, componentMember, bindingState, bindingContext ),
				property,
				basicValue,
				memberTarget,
				basicValueIntent,
				columnNamingPatterns,
				uniqueByDefault,
				nullableByDefault,
				basicValueIntent.insertable(),
				updatable && basicValueIntent.updatable(),
				bindingOptions,
				bindingState
		);
		applyComponentMemberOptionality( member, componentMember.type(), property, column, nullableByDefault );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.embeddableMember( member, componentMember.type(), basicValueIntent.conversion() ),
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);
		if ( source.kind() == ComponentSource.Kind.EMBEDDED_IDENTIFIER ) {
			applyGeneratedIdentifierMember( basicValue, member, bindingState );
		}
		applyFinalFieldMutability(
				member,
				property,
				basicValue,
				isRecordComponentMember( source.componentType(), member )
		);
		return new MaterializedBasicValue( basicValue, column );
	}

	private static void applyGeneratedIdentifierMember(
			BasicValue basicValue,
			MemberDetails member,
			BindingState bindingState) {
		if ( GeneratorBinder.createIdGeneratorFromGeneratorAnnotation(
				basicValue,
				member,
				bindingState.getMetadataBuildingContext(),
				basicValue.getTable().getName() + "." + member.getName()
		) ) {
			return;
		}
		if ( GeneratorBinder.createIdGeneratorFromGeneratorAnnotation(
				basicValue,
				member,
				member.getDeclaringType(),
				bindingState.getMetadataBuildingContext(),
				basicValue.getTable().getName() + "." + member.getName()
		) ) {
			return;
		}
		final var packageInfo = packageInfoDetails( member, bindingState );
		if ( packageInfo != null && GeneratorBinder.createIdGeneratorFromGeneratorAnnotation(
				basicValue,
				member,
				packageInfo,
				bindingState.getMetadataBuildingContext(),
				basicValue.getTable().getName() + "." + member.getName()
		) ) {
			return;
		}

		final GeneratedValue generatedValue = member.getDirectAnnotationUsage( GeneratedValue.class );
		if ( generatedValue == null ) {
			return;
		}

		final var generationType = generatedValue.strategy() == null ? AUTO : generatedValue.strategy();
		GeneratorBinder.makeIdGenerator(
				basicValue,
				member,
				GeneratorStrategies.generatorStrategy( generationType, generatedValue.generator(), member.getType() ),
				generatedValue.generator(),
				bindingState.getMetadataBuildingContext(),
				localGenerators( member, bindingState )
		);
	}

	private static void validateNonIdentifierGeneratedValue(MemberDetails member, String propertyPath) {
		if ( member.hasDirectAnnotationUsage( GeneratedValue.class ) ) {
			throw new AnnotationException(
					"Property '" + propertyPath + "' is annotated '@GeneratedValue' but is not part of an identifier"
			);
		}
	}

	private static Map<String, IdentifierGeneratorDefinition> localGenerators(
			MemberDetails member,
			BindingState bindingState) {
		final HashMap<String, IdentifierGeneratorDefinition> localGenerators = new HashMap<>();
		visitPackageGeneratorDefinitions( member, bindingState, localGenerators );
		GeneratorBinder.visitIdGeneratorDefinitions(
				member.getDeclaringType(),
				generator -> addLocalGenerator( localGenerators, generator ),
				bindingState.getMetadataBuildingContext()
		);
		GeneratorBinder.visitIdGeneratorDefinitions(
				member,
				generator -> addLocalGenerator( localGenerators, generator ),
				bindingState.getMetadataBuildingContext()
		);
		return localGenerators;
	}

	private static void visitPackageGeneratorDefinitions(
			MemberDetails member,
			BindingState bindingState,
			HashMap<String, IdentifierGeneratorDefinition> localGenerators) {
		final String className = member.getDeclaringType().getClassName();
		if ( className == null ) {
			return;
		}

		final int packageEnd = className.lastIndexOf( '.' );
		if ( packageEnd < 0 ) {
			return;
		}

		final var packageInfo = packageInfoDetails( member, bindingState );
		if ( packageInfo == null ) {
			return;
		}

		GeneratorBinder.visitIdGeneratorDefinitions(
				packageInfo,
				generator -> addLocalGenerator( localGenerators, generator ),
				bindingState.getMetadataBuildingContext()
		);
	}

	private static org.hibernate.models.spi.ClassDetails packageInfoDetails(
			MemberDetails member,
			BindingState bindingState) {
		final String className = member.getDeclaringType().getClassName();
		if ( className == null ) {
			return null;
		}

		final int packageEnd = className.lastIndexOf( '.' );
		if ( packageEnd < 0 ) {
			return null;
		}

		final var classDetailsRegistry = bindingState.getMetadataBuildingContext()
				.getBootstrapContext()
				.getModelsContext()
				.getClassDetailsRegistry();
		final String packageInfoName = className.substring( 0, packageEnd ) + ".package-info";
		var packageInfo = classDetailsRegistry.findClassDetails( packageInfoName );
		if ( packageInfo == null ) {
			try {
				packageInfo = classDetailsRegistry.resolveClassDetails( packageInfoName );
			}
			catch (ClassLoadingException ignored) {
				return null;
			}
		}
		return packageInfo;
	}

	private static void addLocalGenerator(
			HashMap<String, IdentifierGeneratorDefinition> localGenerators,
			IdentifierGeneratorDefinition generator) {
		if ( generator.getName().isEmpty() ) {
			localGenerators.put( "\u0000" + localGenerators.size(), generator );
		}
		else {
			localGenerators.put( generator.getName(), generator );
		}
	}

	private static void applyFinalFieldMutability(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			boolean updateableRecordMember) {
		if ( !isFinalField( member ) ) {
			return;
		}
		final var type = basicValue.getType();
		if ( !updateableRecordMember && type != null && !type.isMutable() ) {
			property.setUpdatable( false );
		}
		property.setMutable( false );
	}

	private static boolean isRecordComponentMember(ClassDetails componentType, MemberDetails member) {
		return member.getDeclaringType() == componentType
			&& ( member.getKind() == AnnotationTarget.Kind.RECORD_COMPONENT
				|| componentType.findRecordComponentByName( member.getName() ) != null );
	}

	private static boolean isFinalField(MemberDetails member) {
		if ( !member.isField() ) {
			return false;
		}
		if ( member.isFinal() ) {
			return true;
		}
		if ( !member.getDeclaringType().isImplementor( Managed.class ) ) {
			return false;
		}
		return originalClassFileDeclaresFinalField( member );
	}

	private static boolean originalClassFileDeclaresFinalField(MemberDetails member) {
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		if ( originalClassFileDeclaresFinalField( member, contextClassLoader ) ) {
			return true;
		}
		return originalClassFileDeclaresFinalField( member, BasicValueMappingMaterializer.class.getClassLoader() );
	}

	private static boolean originalClassFileDeclaresFinalField(MemberDetails member, ClassLoader classLoader) {
		if ( classLoader == null ) {
			return false;
		}
		final String resourceName = member.getDeclaringType().getName().replace( '.', '/' ) + ".class";
		try ( InputStream inputStream = classLoader.getResourceAsStream( resourceName ) ) {
			return inputStream != null && classFileDeclaresFinalField( inputStream, member.getName() );
		}
		catch (IOException ignored) {
			return false;
		}
	}

	private static boolean classFileDeclaresFinalField(InputStream inputStream, String fieldName) throws IOException {
		final DataInputStream data = new DataInputStream( inputStream );
		if ( data.readInt() != 0xCAFEBABE ) {
			return false;
		}
		data.readUnsignedShort();
		data.readUnsignedShort();
		final String[] utf8Constants = readUtf8Constants( data );
		data.readUnsignedShort();
		data.readUnsignedShort();
		data.readUnsignedShort();
		final int interfacesCount = data.readUnsignedShort();
		for ( int i = 0; i < interfacesCount; i++ ) {
			data.readUnsignedShort();
		}
		final int fieldsCount = data.readUnsignedShort();
		for ( int i = 0; i < fieldsCount; i++ ) {
			final int accessFlags = data.readUnsignedShort();
			final String name = utf8Constants[data.readUnsignedShort()];
			data.readUnsignedShort();
			skipAttributes( data );
			if ( fieldName.equals( name ) ) {
				return ( accessFlags & 0x0010 ) != 0;
			}
		}
		return false;
	}

	private static String[] readUtf8Constants(DataInputStream data) throws IOException {
		final int constantPoolCount = data.readUnsignedShort();
		final String[] utf8Constants = new String[constantPoolCount];
		for ( int i = 1; i < constantPoolCount; i++ ) {
			final int tag = data.readUnsignedByte();
			switch ( tag ) {
				case 1 -> utf8Constants[i] = data.readUTF();
				case 3, 4 -> data.skipBytes( 4 );
				case 5, 6 -> {
					data.skipBytes( 8 );
					i++;
				}
				case 7, 8, 16, 19, 20 -> data.skipBytes( 2 );
				case 9, 10, 11, 12, 17, 18 -> data.skipBytes( 4 );
				case 15 -> data.skipBytes( 3 );
				default -> throw new IOException( "Unsupported class-file constant-pool tag " + tag );
			}
		}
		return utf8Constants;
	}

	private static void skipAttributes(DataInputStream data) throws IOException {
		final int attributesCount = data.readUnsignedShort();
		for ( int i = 0; i < attributesCount; i++ ) {
			data.readUnsignedShort();
			final int length = data.readInt();
			data.skipBytes( length );
		}
	}

	private static Table resolveTable(
			BasicValueIntent basicValueIntent,
			PersistentClass ownerBinding,
			ComponentMemberTarget memberTarget) {
		final String tableName = basicValueIntent.columnSource() == null
				? basicValueIntent.tableName()
				: basicValueIntent.columnSource().table();
		if ( tableName == null || tableName.isEmpty() ) {
			return memberTarget.table();
		}
		return ownerBinding.getTable( tableName );
	}

	private Column bindComponentMemberColumn(
			Supplier<String> implicitName,
			Property property,
			BasicValue basicValue,
			ComponentMemberTarget memberTarget,
			BasicValueIntent basicValueIntent,
			List<String> columnNamingPatterns,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean insertable,
			boolean updatable,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		final Column column = ColumnBinder.bindColumn(
				basicValueIntent.columnSource(),
				implicitName,
				uniqueByDefault,
				nullableByDefault
		);
		final String logicalName = applyColumnNamingPatterns( column.getName(), columnNamingPatterns );
		column.setName( ColumnBinder.finalizeColumnName( logicalName, bindingOptions, bindingState ) );
		applyColumnTransformer( basicValueIntent, property, column );
		applyChecks( basicValueIntent, column );
		basicValue.addColumn( column, insertable, updatable );
		memberTarget.registerMemberColumn( column );
		registerTableColumn( basicValue, memberTarget, column );
		if ( !memberTarget.isAggregateMemberTarget() ) {
			ColumnBinder.registerColumnNameBinding(
					basicValue.getTable(),
					logicalName,
					column,
					bindingOptions,
					bindingState
			);
		}
		return column;
	}

	private static void registerTableColumn(BasicValue basicValue, ComponentMemberTarget memberTarget, Column column) {
		if ( !memberTarget.isAggregateMemberTarget() ) {
			basicValue.getTable().addColumn( column );
		}
	}

	private static String implicitBasicColumnName(
			ComponentSource source,
			ComponentMemberBinding member,
			BindingState bindingState,
			BindingContext bindingContext) {
		return bindingContext.getImplicitNamingStrategy()
				.determineBasicColumnName( new ImplicitBasicColumnNameSource() {
					@Override
					public AttributePath getAttributePath() {
						return member.namingPath();
					}

					@Override
					public boolean isCollectionElement() {
						return false;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return bindingState.getMetadataBuildingContext();
					}
				} )
				.getText();
	}

	private static String applyColumnNamingPatterns(String name, List<String> patterns) {
		if ( patterns.isEmpty() ) {
			return name;
		}

		String result = name;
		for ( int i = patterns.size() - 1; i >= 0; i-- ) {
			final String pattern = patterns.get( i );
			if ( isNotEmpty( pattern ) ) {
				result = String.format( Locale.ROOT, pattern, result );
			}
		}
		return result;
	}

	private static Formula formula(BasicValueIntent basicValueIntent) {
		final Formula formula = new Formula( basicValueIntent.formulaExpression() );
		formula.setSelectableName( basicValueIntent.formulaSelectableName() );
		return formula;
	}

	private static void applyBasicOptionality(MemberDetails member, Property property, Column column) {
		final Basic basic = member.getDirectAnnotationUsage( Basic.class );
		final boolean optionalByType = member.getType().getTypeKind() != TypeDetails.Kind.PRIMITIVE;
		final boolean optionalByBasic = basic == null || basic.optional();
		final boolean optionalByColumn = column == null || column.isNullable();
		applyOptionality( property, column, optionalByType && optionalByBasic && optionalByColumn && allowsNull( member ) );
	}

	private static void applyBasicFetch(MemberDetails member, Property property) {
		final Basic basic = member.getDirectAnnotationUsage( Basic.class );
		property.setLazy( basic != null && basic.fetch() == FetchType.LAZY );
	}

	private static void applyBasicOptionality(MemberDetails member, TypeDetails memberType, Property property, Column column) {
		final Basic basic = member.getDirectAnnotationUsage( Basic.class );
		final boolean optionalByType = memberType.getTypeKind() != TypeDetails.Kind.PRIMITIVE;
		final boolean optionalByBasic = basic == null || basic.optional();
		final boolean optionalByColumn = column == null || column.isNullable();
		applyOptionality( property, column, optionalByType && optionalByBasic && optionalByColumn && allowsNull( member ) );
	}

	private static void applyComponentMemberOptionality(
			MemberDetails member,
			TypeDetails memberType,
			Property property,
			Column column,
			boolean nullableByDefault) {
		final Basic basic = member.getDirectAnnotationUsage( Basic.class );
		final boolean optionalByType = nullableByDefault || memberType.getTypeKind() != TypeDetails.Kind.PRIMITIVE;
		final boolean optionalByBasic = basic == null || basic.optional();
		final boolean optionalByColumn = column == null || column.isNullable();
		applyOptionality( property, column, optionalByType && optionalByBasic && optionalByColumn && allowsNull( member ) );
	}

	private static boolean allowsNull(MemberDetails member) {
		return !member.hasDirectAnnotationUsage( Nonnull.class );
	}

	private static void applyOptionality(Property property, Column column, boolean optional) {
		property.setOptional( optional );
		if ( !optional && column != null ) {
			column.setNullable( false );
		}
	}

	/// Result of materializing a basic value from a value intent.
	///
	/// Formula-valued basics have no materialized [Column], so [#column()] is
	/// nullable.
	public record MaterializedBasicValue(BasicValue value, Column column) {
	}
}
