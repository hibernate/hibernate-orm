/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.loader.internal.AliasConstantsHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.sql.Template;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Collections.unmodifiableList;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.lastIndexOfLetter;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.safeInterning;

/**
 * A mapping model object representing a {@linkplain jakarta.persistence.Column column}
 * of a relational database {@linkplain Table table}.
 *
 * @author Gavin King
 */
public class Column implements Selectable, Serializable, Cloneable, ColumnTypeInformation {

	private Long length;
	private Integer precision;
	private Integer scale;
	private Value value;
	private int typeIndex;
	private String name;
	private boolean nullable = true;
	private boolean unique;
	private String sqlTypeName;
	private Integer sqlTypeCode;
	private boolean sqlTypeLob;
	private boolean quoted;
	int uniqueInteger;
	private String comment;
	private String defaultValue;
	private String generatedAs;
	private String assignmentExpression;
	private String customWrite;
	private String customRead;
	private Size columnSize;
	private String specializedTypeDeclaration;
	private java.util.List<CheckConstraint> checkConstraints = new ArrayList<>();

	public Column() {
	}

	public Column(String columnName) {
		setName( columnName );
	}

	public Long getLength() {
		return length;
	}

	public void setLength(Long length) {
		this.length = length;
	}

	public void setLength(Integer length) {
		this.length = length.longValue();
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if ( isQuoted( name ) ) {
			quoted = true;
			this.name = name.substring( 1, name.length() - 1 );
		}
		else {
			this.name = name;
		}
	}

	private static boolean isQuoted(String name) {
		//TODO: deprecated, remove eventually
		return name != null
			&& name.length() >= 2
			&& isOpenQuote( name.charAt( 0 ) )
			&& isCloseQuote( name.charAt( name.length() - 1 ) );
	}

	private static boolean isOpenQuote(char ch) {
		return Dialect.QUOTE.indexOf( ch ) > -1;
	}

	private static boolean isCloseQuote(char ch) {
		return Dialect.CLOSED_QUOTE.indexOf( ch ) > -1;
	}

	/**
	 * @return the quoted name as it would occur in the mapping file
	 */
	public String getQuotedName() {
		return safeInterning(
				quoted ?
				"`" + name + "`" :
				name
		);
	}

	/**
	 * @return the quoted name using the quoting syntax of the given dialect
	 */
	public String getQuotedName(Dialect dialect) {
		return safeInterning(
				quoted ?
				dialect.openQuote() + name + dialect.closeQuote() :
				name
		);
	}

	@Override
	public String getAlias(Dialect dialect) {
		final int lastLetter = lastIndexOfLetter( name );
		final String suffix = AliasConstantsHelper.get( uniqueInteger );

		String alias = name.toLowerCase( Locale.ROOT );
		if ( lastLetter == -1 ) {
			alias = "column";
		}
		else if ( alias.length() > lastLetter + 1 ) {
			alias = alias.substring( 0, lastLetter + 1 );
		}

		boolean useRawName = name.length() + suffix.length() <= dialect.getMaxAliasLength()
				&& !quoted
				// TODO: get the row id name from the Dialect
				&& !name.equalsIgnoreCase( "rowid" );
		if ( !useRawName ) {
			if ( suffix.length() >= dialect.getMaxAliasLength() ) {
				throw new MappingException(
						String.format(
								"Unique suffix [%s] length must be less than maximum [%d]",
								suffix, dialect.getMaxAliasLength()
						)
				);
			}
			if ( alias.length() + suffix.length() > dialect.getMaxAliasLength() ) {
				alias = alias.substring( 0, dialect.getMaxAliasLength() - suffix.length() );
			}
		}
		return alias + suffix;
	}

	/**
	 * Generate a column alias that is unique across multiple tables
	 */
	@Override
	public String getAlias(Dialect dialect, Table table) {
		return safeInterning(
				getAlias( dialect )
						+ AliasConstantsHelper.get( table.getUniqueInteger() )
		);
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public int getTypeIndex() {
		return typeIndex;
	}

	public void setTypeIndex(int typeIndex) {
		this.typeIndex = typeIndex;
	}

	public boolean isUnique() {
		return unique;
	}

	@Override
	public int hashCode() {
		//used also for generation of FK names!
		return isQuoted()
				? name.hashCode()
				: name.toLowerCase( Locale.ROOT ).hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof Column
			&& equals( (Column) object );
	}

	public boolean equals(Column column) {
		return column != null
			&& ( this == column || isQuoted()
				? name.equals( column.name )
				: name.equalsIgnoreCase( column.name ) );
	}

	public int getSqlTypeCode(Mapping mapping) throws MappingException {
		if ( sqlTypeCode == null ) {
			final Type type = getValue().getType();
			int[] sqlTypeCodes;
			try {
				sqlTypeCodes = type.getSqlTypeCodes( mapping );
			}
			catch ( Exception cause ) {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"Unable to resolve JDBC type code for column '%s' of table '%s'",
								getName(),
								getValue().getTable().getName()
						),
						cause
				);
			}
			final int index = getTypeIndex();
			if ( index >= sqlTypeCodes.length ) {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"Unable to resolve JDBC type code for column '%s' of table '%s'",
								getName(),
								getValue().getTable().getName()
						)
				);
			}
			sqlTypeCode = sqlTypeCodes[index];
		}
		return sqlTypeCode;
	}

	private String getSqlTypeName(DdlTypeRegistry ddlTypeRegistry, Dialect dialect, Mapping mapping) {
		if ( sqlTypeName == null ) {
			final Type type = getValue().getType();
			try {
				if ( isArray( type ) ) {
					sqlTypeName = dialect.getArrayTypeName( getArrayElementTypeName( dialect, ddlTypeRegistry, getArrayElementType( type ) ) );
					sqlTypeLob = false;
				}
				else {
					final int typeCode = getSqlTypeCode( mapping );
					final DdlType descriptor = ddlTypeRegistry.getDescriptor( typeCode );
					if ( descriptor == null ) {
						throw new MappingException(
								String.format(
										Locale.ROOT,
										"Unable to determine SQL type name for column '%s' of table '%s' because there is no type mapping for org.hibernate.type.SqlTypes code: %s (%s)",
										getName(),
										getValue().getTable().getName(),
										typeCode,
										JdbcTypeNameMapper.getTypeName( typeCode )
								)
						);
					}
					final Size size = getColumnSize( dialect, mapping );
					sqlTypeName = ddlTypeRegistry.getTypeName( typeCode, size );
					sqlTypeLob = descriptor.isLob( size );
				}
			}
			catch ( MappingException cause ) {
				throw cause;
			}
			catch ( Exception cause ) {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"Unable to determine SQL type name for column '%s' of table '%s'",
								getName(),
								getValue().getTable().getName()
						),
						cause
				);
			}
		}
		return sqlTypeName;
	}

	private String getArrayElementTypeName(Dialect dialect, DdlTypeRegistry ddlTypeRegistry, BasicType<?> elementType) {
		return ddlTypeRegistry.getTypeName(
				elementType.getJdbcType().getDdlTypeCode(),
				dialect.getSizeStrategy().resolveSize(
						elementType.getJdbcMapping().getJdbcType(),
						elementType.getJavaTypeDescriptor(),
						precision,
						scale,
						length
				)
		);
	}

	private static BasicType<?> getArrayElementType(Type arrayType) {
		final BasicPluralType<?, ?> containerType = (BasicPluralType<?, ?>) arrayType;
		return containerType.getElementType();
	}

	private static boolean isArray(Type type) {
		return type instanceof BasicPluralType<?,?>
			&& ((BasicType<?>) type).getJdbcType() instanceof ArrayJdbcType;
	}

	/**
	 * Returns {@linkplain org.hibernate.type.SqlTypes SQL type code}
	 * for this column, or {@code null} if the type code is unknown.
	 * <p>
	 * Use {@link #getSqlTypeCode(Mapping)} to retrieve the type code
	 * using {@link Value} associated with the column.
	 *
	 * @return the type code, if it is set, otherwise null.
	 */
	public Integer getSqlTypeCode() {
		return sqlTypeCode;
	}

	public void setSqlTypeCode(Integer typeCode) {
		if ( sqlTypeCode != null && !Objects.equals( sqlTypeCode, typeCode ) ) {
			throw new AssertionFailure( "conflicting type codes" );
		}
		sqlTypeCode = typeCode;
	}

	public String getSqlType(Metadata mapping) {
		final Database database = mapping.getDatabase();
		return getSqlTypeName( database.getTypeConfiguration().getDdlTypeRegistry(), database.getDialect(), mapping );
	}

	/**
	 * @deprecated use {@link #getSqlType(Metadata)}
	 */
	@Deprecated(since = "6.2")
	public String getSqlType(TypeConfiguration typeConfiguration, Dialect dialect, Mapping mapping) {
		return getSqlTypeName( typeConfiguration.getDdlTypeRegistry(), dialect, mapping );
	}

	@Override
	public String getTypeName() {
		return sqlTypeName;
	}

	@Override
	public TruthValue getNullable() {
		return nullable ? TruthValue.TRUE : TruthValue.FALSE;
	}

	@Override
	public int getTypeCode() {
		return sqlTypeCode == null ? Types.OTHER : sqlTypeCode;
	}

	@Override
	public int getColumnSize() {
		if ( length == null ) {
			return precision == null ? 0 : precision;
		}
		else {
			return length.intValue();
		}
	}

	@Override
	public int getDecimalDigits() {
		return scale == null ? 0 : scale;
	}

	public Size getColumnSize(Dialect dialect, Mapping mapping) {
		if ( columnSize == null ) {
			Type type = getValue().getType();
			if ( type instanceof EntityType ) {
				type = getTypeForEntityValue( mapping, type, getTypeIndex() );
			}
			if ( type instanceof ComponentType ) {
				type = getTypeForComponentValue( mapping, type, getTypeIndex() );
			}
			if ( type == null ) {
				throw new AssertionFailure( "no typing information available to determine column size" );
			}
			final JdbcMapping jdbcMapping = (JdbcMapping) type;
			columnSize = dialect.getSizeStrategy().resolveSize(
					jdbcMapping.getJdbcType(),
					jdbcMapping.getJdbcJavaType(),
					precision,
					scale,
					length
			);
		}
		return columnSize;
	}

	private Type getTypeForComponentValue(Mapping mapping, Type type, int typeIndex) {
		final Type[] subtypes = ( (ComponentType) type ).getSubtypes();
		int typeStartIndex = 0;
		for ( Type subtype : subtypes ) {
			final int columnSpan = subtype.getColumnSpan(mapping);
			if ( typeStartIndex + columnSpan > typeIndex ) {
				final int subtypeIndex = typeIndex - typeStartIndex;
				if ( subtype instanceof EntityType ) {
					return getTypeForEntityValue(mapping, subtype, subtypeIndex);
				}
				if ( subtype instanceof ComponentType ) {
					return getTypeForComponentValue(mapping, subtype, subtypeIndex);
				}
				if ( subtypeIndex == 0 ) {
					return subtype;
				}
				break;
			}
			typeStartIndex += columnSpan;
		}

		throw new MappingException(
				String.format(
						Locale.ROOT,
						"Unable to resolve Hibernate type for column '%s' of table '%s'",
						getName(),
						getValue().getTable().getName()
				)
		);
	}

	private Type getTypeForEntityValue(Mapping mapping, Type type, int typeIndex) {
		int index = 0;
		if ( type instanceof EntityType ) {
			final EntityType entityType = (EntityType) type;
			return getTypeForEntityValue( mapping, entityType.getIdentifierOrUniqueKeyType( mapping ), typeIndex );
		}
		else if ( type instanceof ComponentType ) {
			for ( Type subtype : ((ComponentType) type).getSubtypes() ) {
				final Type result = getTypeForEntityValue( mapping, subtype, typeIndex - index );
				if ( result != null ) {
					return result;
				}
				index += subtype.getColumnSpan( mapping );
			}
			return null;
		}
		else if ( typeIndex == 0 ) {
			return type;
		}
		else  {
			return null;
		}
	}

	public String getSqlType() {
		return sqlTypeName;
	}

	public void setSqlType(String typeName) {
		if ( sqlTypeName != null && !Objects.equals( sqlTypeName, typeName ) ) {
			throw new AssertionFailure( "conflicting type names" );
		}
		sqlTypeName = typeName;
	}

	public boolean isSqlTypeLob() {
		return sqlTypeLob;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isQuoted() {
		return quoted;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '(' + getName() + ')';
	}

	public void setSpecializedTypeDeclaration(String specializedTypeDeclaration) {
		this.specializedTypeDeclaration = specializedTypeDeclaration;
	}

	public String getSpecializedTypeDeclaration() {
		return specializedTypeDeclaration;
	}

	public boolean hasSpecializedTypeDeclaration() {
		return specializedTypeDeclaration != null;
	}

	public void addCheckConstraint(CheckConstraint checkConstraint) {
		if ( !checkConstraints.contains( checkConstraint) ) {
			checkConstraints.add( checkConstraint );
		}
	}

	public java.util.List<CheckConstraint> getCheckConstraints() {
		return unmodifiableList( checkConstraints );
	}

	@Deprecated(since = "6.2")
	public String getCheckConstraint() {
		if ( checkConstraints.isEmpty() ) {
			return null;
		}
		else if ( checkConstraints.size() > 1 ) {
			throw new IllegalStateException( "column has multiple check constraints" );
		}
		else {
			return checkConstraints.get(0).getConstraint();
		}
	}

	@Deprecated(since = "6.2")
	public void setCheckConstraint(String constraint) {
		if ( constraint != null ) {
			if ( !checkConstraints.isEmpty() ) {
				throw new IllegalStateException( "column already has a check constraint" );
			}
			checkConstraints.add( new CheckConstraint( constraint ) );
		}
	}

	public boolean hasCheckConstraint() {
		return !checkConstraints.isEmpty();
	}

	@Deprecated(since = "6.2")
	public String checkConstraint() {
		if ( checkConstraints.isEmpty() ) {
			return null;
		}
		else if ( checkConstraints.size() > 1 ) {
			throw new IllegalStateException( "column has multiple check constraints" );
		}
		else {
			return checkConstraints.get(0).constraintString();
		}
	}

	@Override
	public String getTemplate(Dialect dialect, TypeConfiguration typeConfiguration, SqmFunctionRegistry registry) {
		return safeInterning(
				hasCustomRead()
					// see note in renderTransformerReadFragment wrt access to SessionFactory
					? Template.renderTransformerReadFragment( customRead, getQuotedName( dialect ) )
					: Template.TEMPLATE + '.' + getQuotedName( dialect )
		);
	}

	public boolean hasCustomRead() {
		return customRead != null;
	}

	public String getReadExpr(Dialect dialect) {
		return hasCustomRead() ? customRead : getQuotedName( dialect );
	}

	@Override
	public String getWriteExpr() {
		return customWrite != null && customWrite.length() > 0 ? customWrite : "?";
	}

	@Override
	public boolean isFormula() {
		return false;
	}

	@Override
	public String getText(Dialect d) {
		return assignmentExpression != null ? assignmentExpression : getQuotedName( d );
	}

	@Override
	public String getText() {
		return assignmentExpression != null ? assignmentExpression : getName();
	}

	@Override
	public String getCustomReadExpression() {
		return customRead;
	}

	@Override
	public String getCustomWriteExpression() {
		return customWrite;
	}

	public Integer getPrecision() {
		return precision;
	}

	public void setPrecision(Integer precision) {
		this.precision = precision;
	}

	public Integer getScale() {
		return scale;
	}

	public void setScale(Integer scale) {
		this.scale = scale;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getGeneratedAs() {
		return generatedAs;
	}

	public void setGeneratedAs(String generatedAs) {
		this.generatedAs = generatedAs;
	}

	public String getAssignmentExpression() {
		return assignmentExpression;
	}

	public void setAssignmentExpression(String assignmentExpression) {
		this.assignmentExpression = assignmentExpression;
	}

	public String getCustomWrite() {
		return customWrite;
	}

	public void setCustomWrite(String customWrite) {
		this.customWrite = safeInterning( customWrite );
	}

	public String getCustomRead() {
		return customRead;
	}

	public void setResolvedCustomRead(String customRead) {
		assert customRead == null || ! isEmpty( customRead.trim() );
		this.customRead = safeInterning( customRead );
	}

	public void setCustomRead(String customRead) {
		this.customRead = safeInterning( nullIfEmpty( customRead ) );
	}

	public String getCanonicalName() {
		return quoted ? name : name.toLowerCase( Locale.ROOT );
	}

	/**
	 * Shallow copy, the value is not copied
	 */
	@Override
	public Column clone() {
		Column copy = new Column();
		copy.length = length;
		copy.precision = precision;
		copy.scale = scale;
		copy.value = value;
		copy.typeIndex = typeIndex;
		copy.name = name;
		copy.quoted = quoted;
		copy.nullable = nullable;
		copy.unique = unique;
		copy.sqlTypeName = sqlTypeName;
		copy.sqlTypeCode = sqlTypeCode;
		copy.uniqueInteger = uniqueInteger; //usually useless
		copy.checkConstraints = checkConstraints;
		copy.comment = comment;
		copy.defaultValue = defaultValue;
		copy.generatedAs = generatedAs;
		copy.assignmentExpression = assignmentExpression;
		copy.customRead = customRead;
		copy.customWrite = customWrite;
		copy.specializedTypeDeclaration = specializedTypeDeclaration;
		copy.columnSize = columnSize;
		return copy;
	}

}
