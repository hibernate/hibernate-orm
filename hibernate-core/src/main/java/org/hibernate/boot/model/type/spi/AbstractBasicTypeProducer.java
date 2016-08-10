/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.spi;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.model.type.internal.BasicTypeParametersSiteContextAdapter;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBasicTypeProducer implements BasicTypeProducer {
	private final TypeConfiguration typeConfiguration;
	private BasicTypeSiteContext basicTypeSiteContext;

	protected AbstractBasicTypeProducer(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public BasicTypeProducer injectBasicTypeSiteContext(BasicTypeSiteContext basicTypeSiteContext) {
		this.basicTypeSiteContext = basicTypeSiteContext;
		return this;
	}

	protected BasicTypeSiteContext getBasicTypeSiteContext() {
		return basicTypeSiteContext;
	}

	@Override
	public BasicType produceBasicType() {
		if ( basicTypeSiteContext == null ) {
			return null;
		}

		return typeConfiguration.getBasicTypeRegistry().resolveBasicType(
				basicTypeSiteContext,
				basicTypeSiteContext

		);
		if ( basicTypeSiteContext.getAttributeConverterDefinition() != null ) {

		if ( attributeConverterDescriptor == null ) {
			// this is here to work like legacy.  This should change when we integrate with metamodel to
			// look for SqlTypeDescriptor and JavaTypeDescriptor individually and create the BasicType (well, really
			// keep a registry of [SqlTypeDescriptor,JavaTypeDescriptor] -> BasicType...)
			if ( className == null ) {
				throw new MappingException( "Attribute types for a dynamic entity must be explicitly specified: " + propertyName );
			}
			typeName = ReflectHelper.reflectedPropertyClass( className, propertyName, metadata.getMetadataBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class ) ).getName();
			// todo : to fully support isNationalized here we need do the process hinted at above
			// 		essentially, much of the logic from #buildAttributeConverterTypeAdapter wrt resolving
			//		a (1) SqlTypeDescriptor, a (2) JavaTypeDescriptor and dynamically building a BasicType
			// 		combining them.
			return;
		}

		// we had an AttributeConverter...
		type = buildAttributeConverterTypeAdapter();
	}

	protected Map collectTypeParameters() {
		return basicTypeSiteContext == null ? null : basicTypeSiteContext.getLocalTypeParameters();
	}
}
