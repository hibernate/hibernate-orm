/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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

package org.hibernate.spatial.dialect.oracle;

import java.sql.Types;

import org.geolatte.geom.codec.db.oracle.OracleJDBCTypeFactory;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Descriptor for the Oracle Spatial SDO_GEOMETRY type
 *
 * @author Karel Maesen, Geovise BVBA
 *
 */
public class SDOGeometryTypeDescriptor implements SqlTypeDescriptor {

	private final OracleJDBCTypeFactory typeFactory;

	/**
	 * Constructs a {@code SqlTypeDescriptor} for the Oracle SDOGeometry type.
	 *
	 * @param typeFactory the type factory to use.
	 */
	public SDOGeometryTypeDescriptor(OracleJDBCTypeFactory typeFactory) {
		this.typeFactory = typeFactory;
	}

	@Override
	public int getSqlType() {
		return Types.STRUCT;
	}

	@Override
	public boolean canBeRemapped() {
		return false;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return (ValueBinder<X>) new SDOGeometryValueBinder( javaTypeDescriptor, this, typeFactory );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return (ValueExtractor<X>) new SDOGeometryValueExtractor( javaTypeDescriptor, this );
	}

	/**
	 * Returns the Oracle type name for SDOGeometry.
	 * @return the Oracle type name
	 */
	public String getTypeName() {
		return "MDSYS.SDO_GEOMETRY";
	}

}
