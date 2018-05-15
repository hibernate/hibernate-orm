/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Locale;
import java.util.function.Supplier;

import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.metamodel.model.relational.spi.Size;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.naming.Identifier;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.sql.Template;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import static org.hibernate.internal.util.StringHelper.safeInterning;

/**
 * A column of a relational database table
 *
 * @author Gavin King
 */
public class Column implements Selectable, Serializable, Cloneable {
	private Identifier tableName;
	private Identifier name;

	private Supplier<SqlTypeDescriptor> sqlTypeDescriptorAccess;
	private JavaTypeMapping javaTypeMapping;

	private String sqlType;

	private int uniqueInteger;

	private boolean quoted;

	private Long length;
	private Integer precision;
	private Integer scale;

	private boolean nullable = true;
	private boolean unique;
	private String checkConstraint;
	private String comment;
	private String defaultValue;
	private String customWrite;
	private String customRead;

	public Column(String columnName, boolean isUnique) {
		this( Identifier.toIdentifier( columnName ), isUnique );
	}

	public Column(Identifier tableName, String columnName, boolean isUnique) {
		this( Identifier.toIdentifier( columnName ), isUnique );
		this.tableName = tableName;
	}

	public Column(Identifier tableName, Identifier columnName, boolean isUnique) {
		this( columnName, isUnique );
		this.tableName = tableName;
	}

	public Column(Identifier columnName, boolean isUnique) {
		setName( columnName );
		setUnique( isUnique );
	}

	public Identifier getName() {
		return name;
	}

	public Identifier getTableName(){
		return tableName;
	}

	public Long getLength() {
		return length;
	}

	public void setLength(Long length) {
		this.length = length;
	}

	public void setTableName(Identifier tableName) {
		this.tableName = tableName;
	}

	public void setName(Identifier columnName) {
		this.name = columnName;
		if ( columnName != null ) {
			this.quoted = columnName.isQuoted();
		}
	}

	public int getUniqueInteger() {
		return uniqueInteger;
	}

	public void setUniqueInteger(int uniqueInteger) {
		this.uniqueInteger = uniqueInteger;
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

	public boolean isUnique() {
		return unique;
	}

	@Override
	public int hashCode() {
		return tableName.hashCode() + name.hashCode();
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

		return tableName.equals( column.tableName ) && name.equals( column.name );
	}

	public String getSqlType() {
		return sqlType;
	}

	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}

	private void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isQuoted() {
		return quoted;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"Boot-model physical Column : %s.%s",
				getTableName(),
				getName()
		);
	}

	public String getCheckConstraint() {
		return checkConstraint;
	}

	public void setCheckConstraint(String checkConstraint) {
		this.checkConstraint = checkConstraint;
	}

	@Override
	public String getTemplate(Dialect dialect, SqmFunctionRegistry functionRegistry) {
		return safeInterning(
				hasCustomRead()
				// see note in renderTransformerReadFragment wrt access to SessionFactory
				? Template.renderTransformerReadFragment( customRead, getQuotedName( dialect ) )
				: Template.TEMPLATE + '.' + name.render( dialect )
		);
	}

	public boolean hasCustomRead() {
		return customRead != null;
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
	public String getText(Dialect dialect) {
		return name.render(dialect);
	}

	@Override
	public String getText() {
		return name.getText();
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptorAccess.get();
	}

	protected BasicJavaDescriptor getJavaTypeDescriptor() {
		return (BasicJavaDescriptor) javaTypeMapping.getJavaTypeDescriptor();
	}

	public void setSqlTypeDescriptorAccess(Supplier<SqlTypeDescriptor> sqlTypeDescriptorAccess) {
		this.sqlTypeDescriptorAccess = sqlTypeDescriptorAccess;
	}

	public JavaTypeMapping getJavaTypeMapping() {
		return javaTypeMapping;
	}

	public void setJavaTypeMapping(JavaTypeMapping javaTypeMapping) {
		this.javaTypeMapping = javaTypeMapping;
	}

	private static final Logger log = Logger.getLogger( Column.class );

	@Override
	public org.hibernate.metamodel.model.relational.spi.PhysicalColumn generateRuntimeColumn(
			Table runtimeTable,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment,
			TypeConfiguration typeConfiguration) {

		final Identifier physicalName = namingStrategy.toPhysicalColumnName(
				getName(),
				jdbcEnvironment
		);

		log.debugf( "Creating runtime column `%s.%s`", runtimeTable.getTableExpression(), physicalName.getText()  );

		final Dialect dialect = jdbcEnvironment.getDialect();
		Size size = new Size.Builder().setLength( getLength() )
				.setPrecision( getPrecision() )
				.setScale( getScale() )
				.build();
		if ( size.getLength() == null
				&& size.getScale() == null && size.getPrecision() == null ) {
			size = dialect.getDefaultSizeStrategy().resolveDefaultSize(
					getSqlTypeDescriptor(),
					getJavaTypeDescriptor()
			);
		}

		String columnSqlType = getSqlType();
		if ( columnSqlType == null ) {
			columnSqlType = dialect.getTypeName( getSqlTypeDescriptor().getJdbcTypeCode(), size );
		}

		final SqlTypeDescriptor sqlTypeDescriptor = getSqlTypeDescriptor();
		final BasicJavaDescriptor javaTypeDescriptor = getJavaTypeDescriptor();

		final PhysicalColumn column = new PhysicalColumn(
				runtimeTable,
				physicalName,
				() -> sqlTypeDescriptor,
				() -> javaTypeDescriptor,
				getDefaultValue(),
				columnSqlType,
				isNullable(),
				isUnique(),
				getComment(),
				typeConfiguration
		);
		column.setSize(	size );
		column.setCheckConstraint( getCheckConstraint() );
		return column;
	}

	public Integer getPrecision() {
		return precision;
	}

	public void setPrecision(Integer scale) {
		this.precision = scale;
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

	public void setCustomRead(String customRead) {
		this.customRead = safeInterning( StringHelper.nullIfEmpty( customRead ) );
	}

	public String getCanonicalName() {
		return name.getCanonicalName();
	}

	/**
	 * Shallow copy, the value is not copied
	 */
	@Override
	public Column clone() {
		Column copy = new Column( name, unique );
		copy.setTableName( tableName );
		copy.setLength( length );
		copy.setScale( scale );
		copy.setNullable( nullable );
		copy.setPrecision( precision );
		copy.setSqlType( sqlType );
		copy.setUniqueInteger( uniqueInteger ); //usually useless
		copy.setCheckConstraint( checkConstraint );
		copy.setComment( comment );
		copy.setDefaultValue( defaultValue );
		copy.setCustomRead( customRead );
		copy.setCustomWrite( customWrite );
		copy.setSqlTypeDescriptorAccess( sqlTypeDescriptorAccess );
		copy.setJavaTypeMapping( javaTypeMapping );
		return copy;
	}

}
