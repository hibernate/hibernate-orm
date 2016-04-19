/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: MultiplicityType.java 6592 2005-04-28 15:44:16Z oneovthafew $
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.IntegerType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.usertype.CompositeUserType;

public class MultiplicityType implements CompositeUserType {

	private static final String[] PROP_NAMES = new String[] {
			"count", "glarch"
	};
	private static final int[] SQL_TYPES = new int[] {
			IntegerType.INSTANCE.getSqlTypeDescriptor().getSqlType(),
			StringType.INSTANCE.getSqlTypeDescriptor().getSqlType()
	};
	private static final Type[] TYPES = new Type[] {
			IntegerType.INSTANCE,
			new ManyToOneType(
					new TypeFactory.TypeScope() {
						@Override
						public SessionFactoryImplementor resolveFactory() {
							// todo : can we tie this into org.hibernate.type.TypeFactory.TypeScopeImpl() somehow?
							throw new HibernateException( "Cannot access SessionFactory from here" );
						}
					},
					Glarch.class.getName()
			)
	};

	public String[] getPropertyNames() {
		return PROP_NAMES;
	}

	public Type[] getPropertyTypes() {
		return TYPES;
	}

	public int hashCode(Object x) throws HibernateException {
		Multiplicity o = (Multiplicity) x;
		return o.count + o.glarch.hashCode();
	}

	public Object getPropertyValue(Object component, int property) {
		Multiplicity o = (Multiplicity) component;
		return property == 0 ?
				new Integer( o.count ) :
				o.glarch;
	}

	public void setPropertyValue(
			Object component,
			int property,
			Object value) {

		Multiplicity o = (Multiplicity) component;
		if ( property == 0 ) {
			o.count = ( (Integer) value ).intValue();
		}
		else {
			o.glarch = (Glarch) value;
		}
	}

	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	public Class returnedClass() {
		return Multiplicity.class;
	}

	public boolean equals(Object x, Object y) {
		Multiplicity mx = (Multiplicity) x;
		Multiplicity my = (Multiplicity) y;
		if ( mx == my ) {
			return true;
		}
		if ( mx == null || my == null ) {
			return false;
		}
		return mx.count == my.count && mx.glarch == my.glarch;
	}

	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {

		Integer c = IntegerType.INSTANCE.nullSafeGet( rs, names[0], session );
		GlarchProxy g = (GlarchProxy) ( (Session) session ).getTypeHelper().entity( Glarch.class ).nullSafeGet(
				rs,
				names[1],
				session,
				owner
		);
		Multiplicity m = new Multiplicity();
		m.count = c == null ? 0 : c.intValue();
		m.glarch = g;
		return m;
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {

		Multiplicity o = (Multiplicity) value;
		GlarchProxy g;
		Integer c;
		if ( o == null ) {
			g = null;
			c = new Integer( 0 );
		}
		else {
			g = o.glarch;
			c = new Integer( o.count );
		}
		StandardBasicTypes.INTEGER.nullSafeSet( st, c, index, session );
		( (Session) session ).getTypeHelper().entity( Glarch.class ).nullSafeSet( st, g, index + 1, session );

	}

	public Object deepCopy(Object value) {
		if ( value == null ) {
			return null;
		}
		Multiplicity v = (Multiplicity) value;
		Multiplicity m = new Multiplicity();
		m.count = v.count;
		m.glarch = v.glarch;
		return m;
	}

	public boolean isMutable() {
		return true;
	}

	public Object assemble(
			Serializable cached,
			SharedSessionContractImplementor session,
			Object owner) throws HibernateException {
		if ( cached == null ) {
			return null;
		}
		Serializable[] o = (Serializable[]) cached;
		Multiplicity m = new Multiplicity();
		m.count = ( (Integer) o[0] ).intValue();
		m.glarch = o[1] == null ?
				null :
				(GlarchProxy) session.internalLoad( Glarch.class.getName(), o[1], false, false );
		return m;
	}

	public Serializable disassemble(Object value, SharedSessionContractImplementor session)
			throws HibernateException {
		if ( value == null ) {
			return null;
		}
		Multiplicity m = (Multiplicity) value;
		return new Serializable[] {
				new Integer( m.count ),
				ForeignKeys.getEntityIdentifierIfNotUnsaved( Glarch.class.getName(), m.glarch, session )
		};
	}

	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return assemble( disassemble( original, session ), session, owner );
	}

}
