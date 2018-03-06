/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.Type;

import java.util.Objects;

/**
 * A simple-point association (ie. a reference to another entity).
 * @author Gavin King
 */
public abstract class ToOne extends SimpleValue implements Fetchable {
	private FetchMode fetchMode;
	protected String referencedPropertyName;
	private String referencedEntityName;
	private String propertyName;
	private boolean embedded;
	private boolean lazy = true;
	protected boolean unwrapProxy;
	protected boolean referenceToPrimaryKey = true;

	/**
	 * @deprecated Use {@link ToOne#ToOne(MetadataBuildingContext, Table)} instead.
	 */
	@Deprecated
	protected ToOne(MetadataImplementor metadata, Table table) {
		super( metadata, table );
	}

	protected ToOne(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode=fetchMode;
	}

	public abstract void createForeignKey() throws MappingException;
	public abstract Type getType() throws MappingException;

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public void setReferencedPropertyName(String name) {
		referencedPropertyName = name==null ? null : name.intern();
	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName==null ? 
				null : referencedEntityName.intern();
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName==null ?
				null : propertyName.intern();
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		if (referencedEntityName == null) {
			final ClassLoaderService cls = getMetadata().getMetadataBuildingOptions()
					.getServiceRegistry()
					.getService( ClassLoaderService.class );
			referencedEntityName = ReflectHelper.reflectedPropertyClass( className, propertyName, cls ).getName();
		}
	}

	public boolean isTypeSpecified() {
		return referencedEntityName!=null;
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof ToOne && isSame( (ToOne) other );
	}

	public boolean isSame(ToOne other) {
		return super.isSame( other )
				&& Objects.equals( referencedPropertyName, other.referencedPropertyName )
				&& Objects.equals( referencedEntityName, other.referencedEntityName )
				&& embedded == other.embedded;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		if (referencedEntityName==null) {
			throw new MappingException("association must specify the referenced entity");
		}
		return super.isValid( mapping );
	}

	public boolean isLazy() {
		return lazy;
	}
	
	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public boolean isUnwrapProxy() {
		return unwrapProxy;
	}

	public void setUnwrapProxy(boolean unwrapProxy) {
		this.unwrapProxy = unwrapProxy;
	}

	public boolean isReferenceToPrimaryKey() {
		return referenceToPrimaryKey;
	}

	public void setReferenceToPrimaryKey(boolean referenceToPrimaryKey) {
		this.referenceToPrimaryKey = referenceToPrimaryKey;
	}
	
}
