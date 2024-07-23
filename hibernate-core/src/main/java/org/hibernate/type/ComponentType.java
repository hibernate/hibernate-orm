/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.Remove;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.CompositeTypeImplementor;
import org.hibernate.usertype.CompositeUserType;

/**
 * Handles "component" mappings
 *
 * @author Gavin King
 */
public class ComponentType extends AbstractType implements CompositeTypeImplementor, ProcedureParameterExtractionAware {
	private final Class<?> componentClass;
	private final boolean mutable;

	private final String[] propertyNames;
	private final Type[] propertyTypes;
	private final boolean[] propertyNullability;
	private final int[] originalPropertyOrder;
	protected final int propertySpan;
	private final CascadeStyle[] cascade;
	private final FetchMode[] joinedFetch;

	private final boolean isAggregate;
	private final boolean isKey;
	private boolean hasNotNullProperty;
	private final CompositeUserType<Object> compositeUserType;
	private boolean hasNullProperty;

	private EmbeddableValuedModelPart mappingModelPart;

	public ComponentType(Component component, int[] originalPropertyOrder, MetadataBuildingContext buildingContext) {
		this.componentClass = component.isDynamic()
				? Map.class
				: component.getComponentClass();

		this.isAggregate = component.getAggregateColumn() != null;
		this.isKey = component.isKey();
		this.propertySpan = component.getPropertySpan();
		this.originalPropertyOrder = originalPropertyOrder;
		this.propertyNames = new String[propertySpan];
		this.propertyTypes = new Type[propertySpan];
		this.propertyNullability = new boolean[propertySpan];
		this.cascade = new CascadeStyle[propertySpan];
		this.joinedFetch = new FetchMode[propertySpan];

		int i = 0;
		for ( Property property : component.getProperties() ) {
			this.propertyNames[i] = property.getName();
			this.propertyTypes[i] = property.getValue().getType();
			this.propertyNullability[i] = property.isOptional();
			this.cascade[i] = property.getCascadeStyle();
			this.joinedFetch[i] = property.getValue().getFetchMode();
			if ( !property.isOptional() ) {
				hasNotNullProperty = true;
			}
			else {
				hasNullProperty = true;
			}
			i++;
		}

		if ( component.getTypeName() != null ) {
			final ManagedBeanRegistry beanRegistry = buildingContext.getBootstrapContext()
					.getServiceRegistry()
					.getService( ManagedBeanRegistry.class );
			final Class<CompositeUserType<?>> customTypeClass = buildingContext.getBootstrapContext()
					.getClassLoaderAccess()
					.classForName( component.getTypeName() );
			if ( buildingContext.getBuildingOptions().disallowExtensionsInCdi() ) {
				//noinspection unchecked,rawtypes
				this.compositeUserType = (CompositeUserType) FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( customTypeClass );
			}
			else {
				//noinspection unchecked,rawtypes
				this.compositeUserType = (CompositeUserType) beanRegistry.getBean( customTypeClass ).getBeanInstance();
			}
		}
		else {
			this.compositeUserType = null;
		}
		this.mutable = !ReflectHelper.isRecord( componentClass ) && ( compositeUserType == null || compositeUserType.isMutable() );
	}

	private boolean isAggregate() {
		return isAggregate;
	}

	public boolean isKey() {
		return isKey;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		int span = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			span += propertyTypes[i].getColumnSpan( mapping );
		}
		return span;
	}

	@Override
	public int[] getSqlTypeCodes(Mapping mapping) throws MappingException {
		//Not called at runtime so doesn't matter if it's slow :)
		int[] sqlTypes = new int[getColumnSpan( mapping )];
		int n = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			int[] subtypes = propertyTypes[i].getSqlTypeCodes( mapping );
			for ( int subtype : subtypes ) {
				sqlTypes[n++] = subtype;
			}
		}
		return sqlTypes;
	}


	@Override
	public final boolean isComponentType() {
		return true;
	}

	public Class<?> getReturnedClass() {
		return componentClass;
	}

	@Override
	public boolean isSame(Object x, Object y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		// null value and empty component are considered equivalent
		Object[] xvalues = getPropertyValues( x );
		Object[] yvalues = getPropertyValues( y );
		for ( int i = 0; i < propertySpan; i++ ) {
			if ( !propertyTypes[i].isSame( xvalues[i], yvalues[i] ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isEqual(final Object x, final Object y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( compositeUserType != null ) {
			return compositeUserType.equals( x, y );
		}
		// null value and empty component are considered equivalent
		for ( int i = 0; i < propertySpan; i++ ) {
			if ( !propertyTypes[i].isEqual( getPropertyValue( x, i ), getPropertyValue( y, i ) ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isEqual(final Object x, final Object y, final SessionFactoryImplementor factory)
			throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( compositeUserType != null ) {
			return compositeUserType.equals( x, y );
		}
		// null value and empty component are considered equivalent
		for ( int i = 0; i < propertySpan; i++ ) {
			if ( !propertyTypes[i].isEqual( getPropertyValue( x, i ), getPropertyValue( y, i ), factory ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int compare(final Object x, final Object y) {
		if ( x == y ) {
			return 0;
		}
		for ( int i = 0; i < propertySpan; i++ ) {
			int propertyCompare = propertyTypes[i].compare( getPropertyValue( x, i ), getPropertyValue( y, i ) );
			if ( propertyCompare != 0 ) {
				return propertyCompare;
			}
		}
		return 0;
	}

	@Override
	public int compare(Object x, Object y, SessionFactoryImplementor sessionFactory) {
		if ( x == y ) {
			return 0;
		}
		for ( int i = 0; i < propertySpan; i++ ) {
			int propertyCompare = propertyTypes[i].compare( getPropertyValue( x, i ), getPropertyValue( y, i ), sessionFactory );
			if ( propertyCompare != 0 ) {
				return propertyCompare;
			}
		}
		return 0;
	}

	public boolean isMethodOf(Method method) {
		return false;
	}

	@Override
	public int getHashCode(final Object x) {
		if ( compositeUserType != null ) {
			return compositeUserType.hashCode( x );
		}
		int result = 17;
		for ( int i = 0; i < propertySpan; i++ ) {
			Object y = getPropertyValue( x, i );
			result *= 37;
			if ( y != null ) {
				result += propertyTypes[i].getHashCode( y );
			}
		}
		return result;
	}

	@Override
	public int getHashCode(final Object x, final SessionFactoryImplementor factory) {
		if ( compositeUserType != null ) {
			return compositeUserType.hashCode( x );
		}
		int result = 17;
		for ( int i = 0; i < propertySpan; i++ ) {
			Object y = getPropertyValue( x, i );
			result *= 37;
			if ( y != null ) {
				result += propertyTypes[i].getHashCode( y, factory );
			}
		}
		return result;
	}

	@Override
	public boolean isDirty(final Object x, final Object y, final SharedSessionContractImplementor session) throws HibernateException {
		if ( x == y ) {
			return false;
		}
		// null value and empty component are considered equivalent
		for ( int i = 0; i < propertySpan; i++ ) {
			if ( propertyTypes[i].isDirty( getPropertyValue( x, i ), getPropertyValue( y, i ), session ) ) {
				return true;
			}
		}
		return false;
	}

	public boolean isDirty(final Object x, final Object y, final boolean[] checkable, final SharedSessionContractImplementor session)
			throws HibernateException {
		if ( x == y ) {
			return false;
		}
		// null value and empty component are considered equivalent
		int loc = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			int len = propertyTypes[i].getColumnSpan( session.getFactory() );
			if ( len <= 1 ) {
				final boolean dirty = ( len == 0 || checkable[loc] ) &&
						propertyTypes[i].isDirty( getPropertyValue( x, i ), getPropertyValue( y, i ), session );
				if ( dirty ) {
					return true;
				}
			}
			else {
				boolean[] subcheckable = new boolean[len];
				System.arraycopy( checkable, loc, subcheckable, 0, len );
				final boolean dirty = propertyTypes[i].isDirty(
						getPropertyValue( x, i ),
						getPropertyValue( y, i ),
						subcheckable,
						session
				);
				if ( dirty ) {
					return true;
				}
			}
			loc += len;
		}
		return false;
	}

	@Override
	public boolean isModified(
			final Object old,
			final Object current,
			final boolean[] checkable,
			final SharedSessionContractImplementor session) throws HibernateException {
		if ( old == current ) {
			return false;
		}
		// null value and empty components are considered equivalent
		int loc = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			int len = propertyTypes[i].getColumnSpan( session.getFactory() );
			boolean[] subcheckable = new boolean[len];
			System.arraycopy( checkable, loc, subcheckable, 0, len );
			if ( propertyTypes[i].isModified( getPropertyValue( old, i ), getPropertyValue( current, i ), subcheckable, session ) ) {
				return true;
			}
			loc += len;
		}
		return false;

	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int begin, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {

		Object[] subvalues = nullSafeGetValues( value );

		for ( int i = 0; i < propertySpan; i++ ) {
			propertyTypes[i].nullSafeSet( st, subvalues[i], begin, session );
			begin += propertyTypes[i].getColumnSpan( session.getFactory() );
		}
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int begin,
			boolean[] settable,
			SharedSessionContractImplementor session)
			throws HibernateException, SQLException {

		Object[] subvalues = nullSafeGetValues( value );
		int loc = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			int len = propertyTypes[i].getColumnSpan( session.getFactory() );
			//noinspection StatementWithEmptyBody
			if ( len == 0 ) {
				//noop
			}
			else if ( len == 1 ) {
				if ( settable[loc] ) {
					propertyTypes[i].nullSafeSet( st, subvalues[i], begin, session );
					begin++;
				}
			}
			else {
				boolean[] subsettable = new boolean[len];
				System.arraycopy( settable, loc, subsettable, 0, len );
				propertyTypes[i].nullSafeSet( st, subvalues[i], begin, subsettable, session );
				begin += ArrayHelper.countTrue( subsettable );
			}
			loc += len;
		}
	}

	private Object[] nullSafeGetValues(Object value) {
		if ( value == null ) {
			return new Object[propertySpan];
		}
		else {
			return getPropertyValues( value );
		}
	}

	@Override
	public Object getPropertyValue(Object component, int i, SharedSessionContractImplementor session)
			throws HibernateException {
		return getPropertyValue( component, i );
	}

	public Object getPropertyValue(Object component, int i) {
		if ( component == null ) {
			return null;
		}

		if ( component instanceof Object[] ) {
			// A few calls to hashCode pass the property values already in an
			// Object[] (ex: QueryKey hash codes for cached queries).
			// It's easiest to just check for the condition here prior to
			// trying reflection.
			return ( (Object[]) component )[i];
		}
		else {
			return embeddableTypeDescriptor().getValue( component, i );
		}
	}

	@Override
	public Object[] getPropertyValues(Object component, SharedSessionContractImplementor session) {
		return getPropertyValues( component );
	}

	@Override
	public Object[] getPropertyValues(Object component) {
		if (component == null) {
			return new Object[propertySpan];
		}
		else if ( component instanceof Object[] ) {
			// A few calls to hashCode pass the property values already in an
			// Object[] (ex: QueryKey hash codes for cached queries).
			// It's easiest to just check for the condition here prior to
			// trying reflection.
			return (Object[]) component;
		}
		else {
			return mappingModelPart
					.getEmbeddableTypeDescriptor()
					.getValues( component );
		}
	}

	@Override
	public void setPropertyValues(Object component, Object[] values) {
		mappingModelPart.getEmbeddableTypeDescriptor().setValues( component, values );
	}

	@Override
	public Type[] getSubtypes() {
		return propertyTypes;
	}

	@Deprecated(since = "6.2") @Remove
	public Generator[] getPropertyValueGenerationStrategies() {
		return null;
	}

	@Override
	public String getName() {
		return "component" + ArrayHelper.toString( propertyNames );
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		if ( value == null ) {
			return "null";
		}

		Map<String, String> result = new HashMap<>();
		Object[] values = getPropertyValues( value );
		for ( int i = 0; i < propertyTypes.length; i++ ) {
			if ( values[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				result.put( propertyNames[i], "<uninitialized>" );
			}
			else {
				result.put( propertyNames[i], propertyTypes[i].toLoggableString( values[i], factory ) );
			}
		}
		return StringHelper.unqualify( getName() ) + result;
	}

	@Override
	public String[] getPropertyNames() {
		return propertyNames;
	}

	@Override
	public Object deepCopy(Object component, SessionFactoryImplementor factory) {
		if ( component == null ) {
			return null;
		}

		if ( compositeUserType != null ) {
			return compositeUserType.deepCopy( component );
		}
		final Object[] values = getPropertyValues( component );
		for ( int i = 0; i < propertySpan; i++ ) {
			values[i] = propertyTypes[i].deepCopy( values[i], factory );
		}

		Object result = instantiator().instantiate( () -> values, factory );

		//not absolutely necessary, but helps for some
		//equals()/hashCode() implementations
		final PropertyAccess parentAccess = mappingModelPart().getParentInjectionAttributePropertyAccess();
		if ( parentAccess != null ) {
			parentAccess.getSetter().set( result, parentAccess.getGetter().get( component ) );
		}

		return result;
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map<Object, Object> copyCache) {

		if ( original == null ) {
			return null;
		}
		if ( compositeUserType != null ) {
			return compositeUserType.replace( original, target, owner );
		}
		//if ( original == target ) return target;

		final Object[] originalValues = getPropertyValues( original );
		final Object[] resultValues = getPropertyValues( target );
		final Object[] replacedValues = TypeHelper.replace(
				originalValues,
				resultValues,
				propertyTypes,
				session,
				owner,
				copyCache
		);

		if ( target == null || !isMutable() ) {
			return instantiator().instantiate( () -> replacedValues, session.getSessionFactory() );
		}
		else {
			setPropertyValues( target, replacedValues );
			return target;
		}
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map<Object, Object> copyCache,
			ForeignKeyDirection foreignKeyDirection) {

		if ( original == null ) {
			return null;
		}
		if ( compositeUserType != null ) {
			return compositeUserType.replace( original, target, owner );
		}
		//if ( original == target ) return target;


		final Object[] originalValues = getPropertyValues( original );
		final Object[] resultValues = getPropertyValues( target );
		final Object[] replacedValues = TypeHelper.replace(
				originalValues,
				resultValues,
				propertyTypes,
				session,
				owner,
				copyCache,
				foreignKeyDirection
		);

		if ( target == null || !isMutable() ) {
			return instantiator().instantiate( () -> replacedValues, session.getSessionFactory() );
		}
		else {
			setPropertyValues( target, replacedValues );
			return target;
		}
	}

	@Override
	public CascadeStyle getCascadeStyle(int i) {
		return cascade[i];
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {

		if ( value == null ) {
			return null;
		}
		else if ( compositeUserType != null ) {
			return compositeUserType.disassemble( value );
		}
		else {
			Object[] values = getPropertyValues( value );
			for ( int i = 0; i < propertyTypes.length; i++ ) {
				values[i] = propertyTypes[i].disassemble( values[i], session, owner );
			}
			return values;
		}
	}

	@Override
	public Serializable disassemble(Object value, SessionFactoryImplementor sessionFactory) throws HibernateException {
		if ( value == null ) {
			return null;
		}
		else if ( compositeUserType != null ) {
			return compositeUserType.disassemble( value );
		}
		else {
			Object[] values = getPropertyValues( value );
			for ( int i = 0; i < propertyTypes.length; i++ ) {
				values[i] = propertyTypes[i].disassemble( values[i], sessionFactory );
			}
			return values;
		}
	}

	@Override
	public Object assemble(Serializable object, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {

		if ( object == null ) {
			return null;
		}
		else if ( compositeUserType != null ) {
			return compositeUserType.assemble( object, owner );
		}
		else {
			Object[] values = (Object[]) object;
			Object[] assembled = new Object[values.length];
			for ( int i = 0; i < propertyTypes.length; i++ ) {
				assembled[i] = propertyTypes[i].assemble( (Serializable) values[i], session, owner );
			}

			final Object instance = instantiator().instantiate( () -> assembled, session.getFactory() );

			final PropertyAccess parentInjectionAccess = mappingModelPart.getParentInjectionAttributePropertyAccess();
			if ( parentInjectionAccess != null ) {
				parentInjectionAccess.getSetter().set( instance, owner );
			}

			return instance;
		}
	}

	@Override
	public FetchMode getFetchMode(int i) {
		return joinedFetch[i];
	}

	@Override
	public boolean[] getPropertyNullability() {
		return propertyNullability;
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		boolean[] result = new boolean[getColumnSpan( mapping )];
		if ( value == null ) {
			return result;
		}
		Object[] values = getPropertyValues( value ); //TODO!!!!!!!
		int loc = 0;
		for ( int i = 0; i < propertyTypes.length; i++ ) {
			boolean[] propertyNullness = propertyTypes[i].toColumnNullness( values[i], mapping );
			System.arraycopy( propertyNullness, 0, result, loc, propertyNullness.length );
			loc += propertyNullness.length;
		}
		return result;
	}

	@Override
	public boolean isEmbedded() {
		return false;
	}

	@Override
	public int getPropertyIndex(String name) {
		String[] names = getPropertyNames();
		for ( int i = 0, max = names.length; i < max; i++ ) {
			if ( names[i].equals( name ) ) {
				return i;
			}
		}
		throw new PropertyNotFoundException(
				"Unable to locate property named " + name + " on " + getReturnedClass().getName()
		);
	}

	public int[] getOriginalPropertyOrder() {
		return originalPropertyOrder;
	}

	private Boolean canDoExtraction;

	@Override
	public boolean canDoExtraction() {
		if ( canDoExtraction == null ) {
			canDoExtraction = determineIfProcedureParamExtractionCanBePerformed();
		}
		return canDoExtraction;
	}

	@Override
	public JdbcType getJdbcType() {
		final SelectableMapping aggregateMapping = mappingModelPart.getEmbeddableTypeDescriptor().getAggregateMapping();
		return aggregateMapping == null ? null : aggregateMapping.getJdbcMapping().getJdbcType();
	}

	private boolean determineIfProcedureParamExtractionCanBePerformed() {
		for ( Type propertyType : propertyTypes ) {
			if ( !(propertyType instanceof ProcedureParameterExtractionAware) ) {
				return false;
			}
			if ( !( (ProcedureParameterExtractionAware<?>) propertyType ).canDoExtraction() ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Object extract(CallableStatement statement, int startIndex, SharedSessionContractImplementor session) throws SQLException {
		Object[] values;
		if ( isAggregate() ) {
			values = (Object[]) getMappingModelPart().getEmbeddableTypeDescriptor().getAggregateMapping()
					.getJdbcMapping()
					.getJdbcValueExtractor()
					.extract( statement, startIndex, session );
		}
		else {
			values = new Object[propertySpan];
			int currentIndex = startIndex;
			boolean notNull = false;
			for ( int i = 0; i < propertySpan; i++ ) {
				// we know this cast is safe from canDoExtraction
				final Type propertyType = propertyTypes[i];
				final Object value = ( (ProcedureParameterExtractionAware<?>) propertyType )
						.extract( statement, currentIndex, session );
				if ( value == null ) {
					if ( isKey ) {
						return null; //different nullability rules for pk/fk
					}
				}
				else {
					notNull = true;
				}
				values[i] = value;
				currentIndex += propertyType.getColumnSpan( session.getFactory() );
			}

			if ( !notNull ) {
				values = null;
			}
		}

		return resolve( values, session );
	}

	@Override
	public Object extract(CallableStatement statement, String paramName, SharedSessionContractImplementor session)
			throws SQLException {

		assert isAggregate();
		final Object[] values = (Object[]) getMappingModelPart().getEmbeddableTypeDescriptor().getAggregateMapping()
				.getJdbcMapping()
				.getJdbcValueExtractor()
				.extract( statement, paramName, session );

		return resolve( values, session );
	}

	private Object resolve(Object[] value, SharedSessionContractImplementor session) throws HibernateException {
		return instantiator().instantiate( () -> value, session.getFactory() );
	}

	private EmbeddableMappingType embeddableTypeDescriptor() {
		return mappingModelPart.getEmbeddableTypeDescriptor();
	}

	protected final EmbeddableInstantiator instantiator() {
		return embeddableTypeDescriptor().getRepresentationStrategy().getInstantiator();
	}

	@Override
	public boolean hasNotNullProperty() {
		return hasNotNullProperty;
	}

	@Override
	public boolean hasNullProperty() {
		return hasNullProperty;
	}

	@Override
	public Class<?> getBindableJavaType() {
		return getReturnedClass();
	}

	@Override
	public SqmExpressible<?> resolveExpressible(SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getRuntimeMetamodels().getJpaMetamodel().embeddable( getReturnedClass() );
	}

	@Override
	public void injectMappingModelPart(EmbeddableValuedModelPart part, MappingModelCreationProcess process) {
		this.mappingModelPart = part;
	}

	@Override
	public EmbeddableValuedModelPart getMappingModelPart() {
		return mappingModelPart;
	}

	public EmbeddableValuedModelPart mappingModelPart() {
		if ( mappingModelPart == null ) {
			throw new IllegalStateException( "Attempt to access EmbeddableValuedModelPart prior to its injection" );
		}
		return mappingModelPart;
	}

	public boolean isCompositeUserType() {
		return compositeUserType != null;
	}
}
