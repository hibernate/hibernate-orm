/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.internal.AliasConstantsHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.sql.Template;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static org.hibernate.internal.util.StringHelper.safeInterning;

/**
 * A column of a relational database table
 *
 * @author Gavin King
 */
public class Column implements Selectable, Serializable, Cloneable {

	private Long length;
	private Integer precision;
	private Integer scale;
	private Value value;
	private int typeIndex;
	private String name;
	private boolean nullable = true;
	private boolean unique;
	private String sqlType;
	private Integer sqlTypeCode;
	private boolean quoted;
	int uniqueInteger;
	private String checkConstraint;
	private String comment;
	private String defaultValue;
	private String customWrite;
	private String customRead;

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
		if (
				StringHelper.isNotEmpty( name ) &&
						Dialect.QUOTE.indexOf( name.charAt( 0 ) ) > -1 //TODO: deprecated, remove eventually
				) {
			quoted = true;
			this.name = name.substring( 1, name.length() - 1 );
		}
		else {
			this.name = name;
		}
	}

	/**
	 * returns quoted name as it would be in the mapping file.
	 */
	public String getQuotedName() {
		return safeInterning(
				quoted ?
				"`" + name + "`" :
				name
		);
	}

	public String getQuotedName(Dialect d) {
		return safeInterning(
				quoted ?
				d.openQuote() + name + d.closeQuote() :
				name
		);
	}

	@Override
	public String getAlias(Dialect dialect) {
		final int lastLetter = StringHelper.lastIndexOfLetter( name );
		final String suffix = AliasConstantsHelper.get( uniqueInteger );

		String alias = name.toLowerCase( Locale.ROOT );
		if ( lastLetter == -1 ) {
			alias = "column";
		}
		else if ( name.length() > lastLetter + 1 ) {
			alias = name.substring( 0, lastLetter + 1 );
		}

		boolean useRawName = name.length() + suffix.length() <= dialect.getMaxAliasLength()
				&& !quoted && !name.toLowerCase( Locale.ROOT ).equals( "rowid" );
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
		return safeInterning( getAlias( dialect ) + AliasConstantsHelper.get( table.getUniqueInteger() ) );
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
		return isQuoted() ?
				name.hashCode() :
				name.toLowerCase( Locale.ROOT ).hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof Column && equals( (Column) object );
	}

	@SuppressWarnings("SimplifiableIfStatement")
	public boolean equals(Column column) {
		if ( null == column ) {
			return false;
		}
		if ( this == column ) {
			return true;
		}

		return isQuoted() ?
				name.equals( column.name ) :
				name.equalsIgnoreCase( column.name );
	}

	public int getSqlTypeCode(Mapping mapping) throws MappingException {
		org.hibernate.type.Type type = getValue().getType();
		try {
			int sqlTypeCode = type.sqlTypes( mapping )[getTypeIndex()];
			if ( getSqlTypeCode() != null && getSqlTypeCode() != sqlTypeCode ) {
				throw new MappingException( "SQLType code's does not match. mapped as " + sqlTypeCode + " but is " + getSqlTypeCode() );
			}
			return sqlTypeCode;
		}
		catch (Exception e) {
			throw new MappingException(
					"Could not determine type for column " +
							name +
							" of type " +
							type.getClass().getName() +
							": " +
							e.getClass().getName(),
					e
			);
		}
	}

	/**
	 * Returns the underlying columns SqlTypeCode.
	 * If null, it is because the SqlTypeCode is unknown.
	 * <p/>
	 * Use #getSqlTypeCode(Mapping) to retrieve the SqlTypeCode used
	 * for the columns associated Value/Type.
	 *
	 * @return sqlTypeCode if it is set, otherwise null.
	 */
	public Integer getSqlTypeCode() {
		return sqlTypeCode;
	}

	public void setSqlTypeCode(Integer typeCode) {
		sqlTypeCode = typeCode;
	}

	public String getSqlType(Dialect dialect, Mapping mapping) throws HibernateException {
		if ( sqlType == null ) {
			final Size defaultSize = getColumnDefaultSize( dialect, mapping );

			final Size size = getColumnSize( defaultSize );
			try {
				sqlType = dialect.getTypeName( getSqlTypeCode( mapping ), size );
			}
			catch (HibernateException cause) {
				throw new HibernateException(
						String.format(
								Locale.ROOT,
								"Unable to resolve JDBC type code for column `%s.%s`",
								getValue().getTable().getName(),
								getName()
						),
						cause
				);
			}
		}
		return sqlType;
	}

	private Size getColumnSize(Size defaultSize) {
		final Integer columnPrecision = precision != null ? precision : defaultSize.getPrecision();
		final Integer columnScale = scale != null ? scale : defaultSize.getScale();
		final Long columnLength = length != null ? length : defaultSize.getLength();

		return new Size( columnPrecision, columnScale, columnLength, null );
	}

	private Size getColumnDefaultSize(Dialect dialect, Mapping mapping) {
		Type type = getValue().getType();

		if ( type instanceof EntityType ) {
			type = getTypeForEntityValue( mapping, type, getTypeIndex() );
		}
		if ( type instanceof ComponentType ) {
			type = getTypeForComponentValue( mapping, type, getTypeIndex() );
		}
		return dialect.getDefaultSizeStrategy().resolveDefaultSize(
				( (JdbcMapping) type ).getSqlTypeDescriptor(),
				( (JdbcMapping) type ).getJavaTypeDescriptor()
		);
	}

	private Type getTypeForComponentValue(Mapping mapping, Type type, int typeIndex) {
		final Type[] subtypes = ( (ComponentType) type ).getSubtypes();
		int currentSubtypesColumnSpans = 0;
		for ( int i = 0; i <= subtypes.length; i++ ) {
			Type subtype = subtypes[i];
			int subtypeColumnSpan = subtype.getColumnSpan( mapping );
			currentSubtypesColumnSpans += subtypeColumnSpan;
			if ( currentSubtypesColumnSpans - 1 >= typeIndex ) {
				if ( subtype instanceof EntityType ) {
					return getTypeForEntityValue( mapping, subtype, subtypeColumnSpan - i );
				}
				if ( subtype instanceof ComponentType ) {
					return getTypeForComponentValue( mapping, subtype, subtypeColumnSpan - i );
				}
				if ( i == typeIndex ) {
					return subtype;
				}
			}
		}

		throw new HibernateException(
				String.format(
						Locale.ROOT,
						"Unable to resolve org.hibernate.type.Type for column `%s.%s`",
						getValue().getTable().getName(),
						getName()
				)
		);
	}

	private Type getTypeForEntityValue(Mapping mapping, Type type, int typeIndex) {
		while ( !( type instanceof JdbcMapping ) ) {
			//ManyToOneType doesn't implement JdbcMapping
			type = mapping.getIdentifierType( ( (EntityType) type ).getAssociatedEntityName() );
			if ( type instanceof ComponentType ) {
				type = ( (ComponentType) type ).getSubtypes()[typeIndex];
			}
		}
		return type;
	}

	public String getSqlType() {
		return sqlType;
	}

	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isQuoted() {
		return quoted;
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + getName() + ')';
	}

	public String getCheckConstraint() {
		return checkConstraint;
	}

	public void setCheckConstraint(String checkConstraint) {
		this.checkConstraint = checkConstraint;
	}

	public boolean hasCheckConstraint() {
		return checkConstraint != null;
	}

	public String checkConstraint() {
		if (checkConstraint==null) {
			return null;
		}
		return " check (" + checkConstraint + ")";
	}

	@Override
	public String getTemplate(Dialect dialect, SqmFunctionRegistry functionRegistry) {
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

	public String getWriteExpr() {
		return ( customWrite != null && customWrite.length() > 0 ) ? customWrite : "?";
	}

	@Override
	public boolean isFormula() {
		return false;
	}

	@Override
	public String getText(Dialect d) {
		return getQuotedName( d );
	}

	@Override
	public String getText() {
		return getName();
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
		assert customRead == null || ! StringHelper.isEmpty( customRead.trim() );
		this.customRead = safeInterning( customRead );
	}

	public void setCustomRead(String customRead) {
		this.customRead = safeInterning( StringHelper.nullIfEmpty( customRead ) );
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
		copy.setLength( length );
		copy.setScale( scale );
		copy.setValue( value );
		copy.setTypeIndex( typeIndex );
		copy.setName( getQuotedName() );
		copy.setNullable( nullable );
		copy.setPrecision( precision );
		copy.setUnique( unique );
		copy.setSqlType( sqlType );
		copy.setSqlTypeCode( sqlTypeCode );
		copy.uniqueInteger = uniqueInteger; //usually useless
		copy.setCheckConstraint( checkConstraint );
		copy.setComment( comment );
		copy.setDefaultValue( defaultValue );
		copy.setCustomRead( customRead );
		copy.setCustomWrite( customWrite );
		return copy;
	}

}
