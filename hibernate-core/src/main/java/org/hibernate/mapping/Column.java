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
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.naming.Identifier;
import org.hibernate.sql.Template;

/**
 * A column of a relational database table
 *
 * @author Gavin King
 */
public class Column implements Selectable, Serializable, Cloneable {

	public static final int DEFAULT_LENGTH = 255;
	public static final int DEFAULT_PRECISION = 19;
	public static final int DEFAULT_SCALE = 2;

	private int length = DEFAULT_LENGTH;
	private int precision = DEFAULT_PRECISION;
	private int scale = DEFAULT_SCALE;
	private Value value;
	private int typeIndex;
	private Identifier name;
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
		setName( Identifier.toIdentifier( columnName ) );
	}

	public Column(Identifier columnName) {
		setName( columnName );
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public Identifier getName() {
		return name;
	}

	public void setName(Identifier columnName) {
		this.name = columnName;
		this.quoted = columnName.isQuoted();
	}

	public String getQuotedName() {
		return name.render();
	}

	public String getQuotedName(Dialect d) {
		return name.render( d );
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
				name.getText().toLowerCase( Locale.ROOT ).hashCode();
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

		return name.equals( column.name );
	}

	public int getSqlTypeCode(Mapping mapping) throws MappingException {
		Type type = getValue().getType();
		try {
			int sqlTypeCode = type.sqlTypes()[getTypeIndex()];
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
	 * Returns the underlying columns sqltypecode.
	 * If null, it is because the sqltype code is unknown.
	 * <p/>
	 * Use #getSqlTypeCode(Mapping) to retrieve the sqltypecode used
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

	public String getSqlType(Dialect dialect) throws HibernateException {
		if ( sqlType == null ) {
			sqlType = dialect.getTypeName( getSqlTypeCode(), getLength(), getPrecision(), getScale() );
		}
		return sqlType;
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

	@Override
	public String getTemplate(Dialect dialect, SQLFunctionRegistry functionRegistry) {
		return hasCustomRead()
				? Template.renderWhereStringTemplate( customRead, dialect, functionRegistry )
				: Template.TEMPLATE + '.' + name.render( dialect );
	}

	public boolean hasCustomRead() {
		return ( customRead != null && customRead.length() > 0 );
	}

	public String getReadExpr(Dialect dialect) {
		return hasCustomRead() ? customRead : name.render( dialect );
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
		return name.getText();
	}

	@Override
	public String getText() {
		return name.render( Dialect.getDialect() );
	}

	@Override
	public org.hibernate.metamodel.model.relational.spi.Column generateRuntimeColumn(
			org.hibernate.metamodel.model.relational.spi.Table runtimeTable,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment) {

		final Identifier physicalName = namingStrategy.toPhysicalColumnName(
				getName(),
				jdbcEnvironment
		);
		return new PhysicalColumn( runtimeTable, physicalName, sqlTypeCode );
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int scale) {
		this.precision = scale;
	}

	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
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
		this.customWrite = customWrite;
	}

	public String getCustomRead() {
		return customRead;
	}

	public void setCustomRead(String customRead) {
		this.customRead = customRead;
	}

	public String getCanonicalName() {
		return name.getCanonicalName();
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
		copy.setName( name );
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
