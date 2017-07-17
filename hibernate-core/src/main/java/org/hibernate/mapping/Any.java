/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * A Hibernate "any" type (ie. polymorphic association to
 * one-of-several tables).
 * @author Gavin King
 */
public class Any extends SimpleValue {
	private String identifierTypeName;
	private String metaTypeName = "string";

	private BasicTypeResolver keyTypeResolver;

	private BasicTypeResolver discriminatorTypeResolver;

	private Map<Object,String> discriminatorMap;

	public Any(MetadataBuildingContext metadata, Table table) {
		super( metadata, table );
	}

	public String getIdentifierType() {
		return identifierTypeName;
	}

	public void setIdentifierType(String identifierType) {
		this.identifierTypeName = identifierType;
	}

	public void setTypeByReflection(String propertyClass, String propertyName) {}

	public String getMetaType() {
		return metaTypeName;
	}

	public void setMetaType(String type) {
		metaTypeName = type;
	}

	public Map getMetaValues() {
		return discriminatorMap;
	}

	public void setMetaValues(Map<Object,String> discriminatorMap) {
		this.discriminatorMap = discriminatorMap;
	}

	public void setTypeUsingReflection(String className, String propertyName)
		throws MappingException {
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public void setIdentifierTypeResolver(BasicTypeResolver keyTypeResolver) {
		this.keyTypeResolver = keyTypeResolver;
	}

	public void setDiscriminatorTypeResolver(BasicTypeResolver discriminatorTypeResolver) {
		this.discriminatorTypeResolver = discriminatorTypeResolver;
	}

	public void addDiscriminatorMapping(Object discriminatorValue, String mappedEntityName) {
		if ( discriminatorMap == null ) {
			discriminatorMap = new HashMap<>();
		}
		discriminatorMap.put( discriminatorValue, mappedEntityName );
	}

	@Override
	protected void setSqlTypeDescriptorResolver(Column column) {
		column.setSqlTypeDescriptorResolver( new AnySqlTypeDescriptorResolver( columns.size() - 1  ) );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return null;
	}

	public class AnySqlTypeDescriptorResolver implements SqlTypeDescriptorResolver {
		BasicTypeResolver[] typesResolvers = new BasicTypeResolver[2];

		private int index;

		public AnySqlTypeDescriptorResolver(int index) {
			this.index = index;
			typesResolvers[0] = discriminatorTypeResolver;
			typesResolvers[1] = keyTypeResolver;
		}

		@Override
		public SqlTypeDescriptor resolveSqlTypeDescriptor() {
			return typesResolvers[index].resolveBasicType().getColumnDescriptor().getSqlTypeDescriptor();
		}
	}
}
