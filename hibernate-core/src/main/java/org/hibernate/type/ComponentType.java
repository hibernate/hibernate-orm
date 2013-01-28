/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.Node;

import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.tuple.StandardProperty;
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.tuple.component.ComponentTuplizer;

/**
 * Handles "component" mappings
 *
 * @author Gavin King
 */
public class ComponentType extends AbstractType implements CompositeType {

	private final TypeFactory.TypeScope typeScope;
	private final String[] propertyNames;
	private final Type[] propertyTypes;
	private final boolean[] propertyNullability;
	protected final int propertySpan;
	private final CascadeStyle[] cascade;
	private final FetchMode[] joinedFetch;
	private final boolean isKey;

	protected final EntityMode entityMode;
	protected final ComponentTuplizer componentTuplizer;

	public ComponentType(TypeFactory.TypeScope typeScope, ComponentMetamodel metamodel) {
		this.typeScope = typeScope;
		// for now, just "re-flatten" the metamodel since this is temporary stuff anyway (HHH-1907)
		this.isKey = metamodel.isKey();
		this.propertySpan = metamodel.getPropertySpan();
		this.propertyNames = new String[ propertySpan ];
		this.propertyTypes = new Type[ propertySpan ];
		this.propertyNullability = new boolean[ propertySpan ];
		this.cascade = new CascadeStyle[ propertySpan ];
		this.joinedFetch = new FetchMode[ propertySpan ];

		for ( int i = 0; i < propertySpan; i++ ) {
			StandardProperty prop = metamodel.getProperty( i );
			this.propertyNames[i] = prop.getName();
			this.propertyTypes[i] = prop.getType();
			this.propertyNullability[i] = prop.isNullable();
			this.cascade[i] = prop.getCascadeStyle();
			this.joinedFetch[i] = prop.getFetchMode();
		}

		this.entityMode = metamodel.getEntityMode();
		this.componentTuplizer = metamodel.getComponentTuplizer();
	}

	public boolean isKey() {
		return isKey;
	}

	public EntityMode getEntityMode() {
		return entityMode;
	}

	public ComponentTuplizer getComponentTuplizer() {
		return componentTuplizer;
	}

	public int getColumnSpan(Mapping mapping) throws MappingException {
		int span = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			span += propertyTypes[i].getColumnSpan( mapping );
		}
		return span;
	}

	public int[] sqlTypes(Mapping mapping) throws MappingException {
		//Not called at runtime so doesn't matter if its slow :)
		int[] sqlTypes = new int[getColumnSpan( mapping )];
		int n = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			int[] subtypes = propertyTypes[i].sqlTypes( mapping );
			for ( int j = 0; j < subtypes.length; j++ ) {
				sqlTypes[n++] = subtypes[j];
			}
		}
		return sqlTypes;
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		//Not called at runtime so doesn't matter if its slow :)
		final Size[] sizes = new Size[ getColumnSpan( mapping ) ];
		int soFar = 0;
		for ( Type propertyType : propertyTypes ) {
			final Size[] propertySizes = propertyType.dictatedSizes( mapping );
			System.arraycopy( propertySizes, 0, sizes, soFar, propertySizes.length );
			soFar += propertySizes.length;
		}
		return sizes;
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		//Not called at runtime so doesn't matter if its slow :)
		final Size[] sizes = new Size[ getColumnSpan( mapping ) ];
		int soFar = 0;
		for ( Type propertyType : propertyTypes ) {
			final Size[] propertySizes = propertyType.defaultSizes( mapping );
			System.arraycopy( propertySizes, 0, sizes, soFar, propertySizes.length );
			soFar += propertySizes.length;
		}
		return sizes;
	}


	@Override
    public final boolean isComponentType() {
		return true;
	}

	public Class getReturnedClass() {
		return componentTuplizer.getMappedClass();
	}

	@Override
    public boolean isSame(Object x, Object y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		Object[] xvalues = getPropertyValues( x, entityMode );
		Object[] yvalues = getPropertyValues( y, entityMode );
		for ( int i = 0; i < propertySpan; i++ ) {
			if ( !propertyTypes[i].isSame( xvalues[i], yvalues[i] ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
    public boolean isEqual(Object x, Object y)
			throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		Object[] xvalues = getPropertyValues( x, entityMode );
		Object[] yvalues = getPropertyValues( y, entityMode );
		for ( int i = 0; i < propertySpan; i++ ) {
			if ( !propertyTypes[i].isEqual( xvalues[i], yvalues[i] ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
    public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory)
			throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		Object[] xvalues = getPropertyValues( x, entityMode );
		Object[] yvalues = getPropertyValues( y, entityMode );
		for ( int i = 0; i < propertySpan; i++ ) {
			if ( !propertyTypes[i].isEqual( xvalues[i], yvalues[i], factory ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
    public int compare(Object x, Object y) {
		if ( x == y ) {
			return 0;
		}
		Object[] xvalues = getPropertyValues( x, entityMode );
		Object[] yvalues = getPropertyValues( y, entityMode );
		for ( int i = 0; i < propertySpan; i++ ) {
			int propertyCompare = propertyTypes[i].compare( xvalues[i], yvalues[i] );
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
    public int getHashCode(Object x) {
		int result = 17;
		Object[] values = getPropertyValues( x, entityMode );
		for ( int i = 0; i < propertySpan; i++ ) {
			Object y = values[i];
			result *= 37;
			if ( y != null ) {
				result += propertyTypes[i].getHashCode( y );
			}
		}
		return result;
	}

	@Override
    public int getHashCode(Object x, SessionFactoryImplementor factory) {
		int result = 17;
		Object[] values = getPropertyValues( x, entityMode );
		for ( int i = 0; i < propertySpan; i++ ) {
			Object y = values[i];
			result *= 37;
			if ( y != null ) {
				result += propertyTypes[i].getHashCode( y, factory );
			}
		}
		return result;
	}

	@Override
    public boolean isDirty(Object x, Object y, SessionImplementor session)
			throws HibernateException {
		if ( x == y ) {
			return false;
		}
		if ( x == null || y == null ) {
			return true;
		}
		Object[] xvalues = getPropertyValues( x, entityMode );
		Object[] yvalues = getPropertyValues( y, entityMode );
		for ( int i = 0; i < xvalues.length; i++ ) {
			if ( propertyTypes[i].isDirty( xvalues[i], yvalues[i], session ) ) {
				return true;
			}
		}
		return false;
	}

	public boolean isDirty(Object x, Object y, boolean[] checkable, SessionImplementor session)
			throws HibernateException {
		if ( x == y ) {
			return false;
		}
		if ( x == null || y == null ) {
			return true;
		}
		Object[] xvalues = getPropertyValues( x, entityMode );
		Object[] yvalues = getPropertyValues( y, entityMode );
		int loc = 0;
		for ( int i = 0; i < xvalues.length; i++ ) {
			int len = propertyTypes[i].getColumnSpan( session.getFactory() );
			if ( len <= 1 ) {
				final boolean dirty = ( len == 0 || checkable[loc] ) &&
				                      propertyTypes[i].isDirty( xvalues[i], yvalues[i], session );
				if ( dirty ) {
					return true;
				}
			}
			else {
				boolean[] subcheckable = new boolean[len];
				System.arraycopy( checkable, loc, subcheckable, 0, len );
				final boolean dirty = propertyTypes[i].isDirty( xvalues[i], yvalues[i], subcheckable, session );
				if ( dirty ) {
					return true;
				}
			}
			loc += len;
		}
		return false;
	}

	@Override
    public boolean isModified(Object old, Object current, boolean[] checkable, SessionImplementor session)
			throws HibernateException {

		if ( current == null ) {
			return old != null;
		}
		if ( old == null ) {
			return current != null;
		}
		Object[] currentValues = getPropertyValues( current, session );
		Object[] oldValues = ( Object[] ) old;
		int loc = 0;
		for ( int i = 0; i < currentValues.length; i++ ) {
			int len = propertyTypes[i].getColumnSpan( session.getFactory() );
			boolean[] subcheckable = new boolean[len];
			System.arraycopy( checkable, loc, subcheckable, 0, len );
			if ( propertyTypes[i].isModified( oldValues[i], currentValues[i], subcheckable, session ) ) {
				return true;
			}
			loc += len;
		}
		return false;

	}

	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return resolve( hydrate( rs, names, session, owner ), session, owner );
	}

	public void nullSafeSet(PreparedStatement st, Object value, int begin, SessionImplementor session)
			throws HibernateException, SQLException {

		Object[] subvalues = nullSafeGetValues( value, entityMode );

		for ( int i = 0; i < propertySpan; i++ ) {
			propertyTypes[i].nullSafeSet( st, subvalues[i], begin, session );
			begin += propertyTypes[i].getColumnSpan( session.getFactory() );
		}
	}

	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int begin,
			boolean[] settable,
			SessionImplementor session)
			throws HibernateException, SQLException {

		Object[] subvalues = nullSafeGetValues( value, entityMode );

		int loc = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			int len = propertyTypes[i].getColumnSpan( session.getFactory() );
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

	private Object[] nullSafeGetValues(Object value, EntityMode entityMode) throws HibernateException {
		if ( value == null ) {
			return new Object[propertySpan];
		}
		else {
			return getPropertyValues( value, entityMode );
		}
	}

	public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {

		return nullSafeGet( rs, new String[] {name}, session, owner );
	}

	public Object getPropertyValue(Object component, int i, SessionImplementor session)
			throws HibernateException {
		return getPropertyValue( component, i, entityMode );
	}

	public Object getPropertyValue(Object component, int i, EntityMode entityMode)
			throws HibernateException {
		return componentTuplizer.getPropertyValue( component, i );
	}

	public Object[] getPropertyValues(Object component, SessionImplementor session)
			throws HibernateException {
		return getPropertyValues( component, entityMode );
	}

	public Object[] getPropertyValues(Object component, EntityMode entityMode)
			throws HibernateException {
		if ( component instanceof Object[] ) {
			// A few calls to hashCode pass the property values already in an 
			// Object[] (ex: QueryKey hash codes for cached queries).
			// It's easiest to just check for the condition here prior to
			// trying reflection.
			return ( Object[] ) component;
		} else {
			return componentTuplizer.getPropertyValues( component );
		}
	}

	public void setPropertyValues(Object component, Object[] values, EntityMode entityMode)
			throws HibernateException {
		componentTuplizer.setPropertyValues( component, values );
	}

	public Type[] getSubtypes() {
		return propertyTypes;
	}

	public String getName() {
		return "component" + ArrayHelper.toString( propertyNames );
	}

	public String toLoggableString(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		if ( value == null ) {
			return "null";
		}
		Map result = new HashMap();
		if ( entityMode == null ) {
			throw new ClassCastException( value.getClass().getName() );
		}
		Object[] values = getPropertyValues( value, entityMode );
		for ( int i = 0; i < propertyTypes.length; i++ ) {
			result.put( propertyNames[i], propertyTypes[i].toLoggableString( values[i], factory ) );
		}
		return StringHelper.unqualify( getName() ) + result.toString();
	}

	public String[] getPropertyNames() {
		return propertyNames;
	}

	public Object deepCopy(Object component, SessionFactoryImplementor factory)
			throws HibernateException {
		if ( component == null ) {
			return null;
		}

		Object[] values = getPropertyValues( component, entityMode );
		for ( int i = 0; i < propertySpan; i++ ) {
			values[i] = propertyTypes[i].deepCopy( values[i], factory );
		}

		Object result = instantiate( entityMode );
		setPropertyValues( result, values, entityMode );

		//not absolutely necessary, but helps for some
		//equals()/hashCode() implementations
		if ( componentTuplizer.hasParentProperty() ) {
			componentTuplizer.setParent( result, componentTuplizer.getParent( component ), factory );
		}

		return result;
	}

	public Object replace(
			Object original,
			Object target,
			SessionImplementor session,
			Object owner,
			Map copyCache)
			throws HibernateException {

		if ( original == null ) {
			return null;
		}
		//if ( original == target ) return target;

		final Object result = target == null
				? instantiate( owner, session )
				: target;

		Object[] values = TypeHelper.replace(
				getPropertyValues( original, entityMode ),
				getPropertyValues( result, entityMode ),
				propertyTypes,
				session,
				owner,
				copyCache
		);

		setPropertyValues( result, values, entityMode );
		return result;
	}

	@Override
    public Object replace(
			Object original,
			Object target,
			SessionImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection)
			throws HibernateException {

		if ( original == null ) {
			return null;
		}
		//if ( original == target ) return target;

		final Object result = target == null ?
				instantiate( owner, session ) :
				target;

		Object[] values = TypeHelper.replace(
				getPropertyValues( original, entityMode ),
				getPropertyValues( result, entityMode ),
				propertyTypes,
				session,
				owner,
				copyCache,
				foreignKeyDirection
		);

		setPropertyValues( result, values, entityMode );
		return result;
	}

	/**
	 * This method does not populate the component parent
	 */
	public Object instantiate(EntityMode entityMode) throws HibernateException {
		return componentTuplizer.instantiate();
	}

	public Object instantiate(Object parent, SessionImplementor session)
			throws HibernateException {

		Object result = instantiate( entityMode );

		if ( componentTuplizer.hasParentProperty() && parent != null ) {
			componentTuplizer.setParent(
					result,
					session.getPersistenceContext().proxyFor( parent ),
					session.getFactory()
			);
		}

		return result;
	}

	public CascadeStyle getCascadeStyle(int i) {
		return cascade[i];
	}

	public boolean isMutable() {
		return true;
	}

	@Override
    public Serializable disassemble(Object value, SessionImplementor session, Object owner)
			throws HibernateException {

		if ( value == null ) {
			return null;
		}
		else {
			Object[] values = getPropertyValues( value, entityMode );
			for ( int i = 0; i < propertyTypes.length; i++ ) {
				values[i] = propertyTypes[i].disassemble( values[i], session, owner );
			}
			return values;
		}
	}

	@Override
    public Object assemble(Serializable object, SessionImplementor session, Object owner)
			throws HibernateException {

		if ( object == null ) {
			return null;
		}
		else {
			Object[] values = ( Object[] ) object;
			Object[] assembled = new Object[values.length];
			for ( int i = 0; i < propertyTypes.length; i++ ) {
				assembled[i] = propertyTypes[i].assemble( ( Serializable ) values[i], session, owner );
			}
			Object result = instantiate( owner, session );
			setPropertyValues( result, assembled, entityMode );
			return result;
		}
	}

	public FetchMode getFetchMode(int i) {
		return joinedFetch[i];
	}

	@Override
    public Object hydrate(
			final ResultSet rs,
			final String[] names,
			final SessionImplementor session,
			final Object owner)
			throws HibernateException, SQLException {

		int begin = 0;
		boolean notNull = false;
		Object[] values = new Object[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			int length = propertyTypes[i].getColumnSpan( session.getFactory() );
			String[] range = ArrayHelper.slice( names, begin, length ); //cache this
			Object val = propertyTypes[i].hydrate( rs, range, session, owner );
			if ( val == null ) {
				if ( isKey ) {
					return null; //different nullability rules for pk/fk
				}
			}
			else {
				notNull = true;
			}
			values[i] = val;
			begin += length;
		}

		return notNull ? values : null;
	}

	@Override
    public Object resolve(Object value, SessionImplementor session, Object owner)
			throws HibernateException {

		if ( value != null ) {
			Object result = instantiate( owner, session );
			Object[] values = ( Object[] ) value;
			Object[] resolvedValues = new Object[values.length]; //only really need new array during semiresolve!
			for ( int i = 0; i < values.length; i++ ) {
				resolvedValues[i] = propertyTypes[i].resolve( values[i], session, owner );
			}
			setPropertyValues( result, resolvedValues, entityMode );
			return result;
		}
		else {
			return null;
		}
	}

	@Override
    public Object semiResolve(Object value, SessionImplementor session, Object owner)
			throws HibernateException {
		//note that this implementation is kinda broken
		//for components with many-to-one associations
		return resolve( value, session, owner );
	}

	public boolean[] getPropertyNullability() {
		return propertyNullability;
	}

	@Override
    public boolean isXMLElement() {
		return true;
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		return xml;
	}

	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) throws HibernateException {
		replaceNode( node, ( Element ) value );
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		boolean[] result = new boolean[ getColumnSpan( mapping ) ];
		if ( value == null ) {
			return result;
		}
		Object[] values = getPropertyValues( value, EntityMode.POJO ); //TODO!!!!!!!
		int loc = 0;
		for ( int i = 0; i < propertyTypes.length; i++ ) {
			boolean[] propertyNullness = propertyTypes[i].toColumnNullness( values[i], mapping );
			System.arraycopy( propertyNullness, 0, result, loc, propertyNullness.length );
			loc += propertyNullness.length;
		}
		return result;
	}

	public boolean isEmbedded() {
		return false;
	}

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
}
