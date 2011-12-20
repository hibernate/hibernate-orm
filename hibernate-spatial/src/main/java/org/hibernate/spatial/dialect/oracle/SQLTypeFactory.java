package org.hibernate.spatial.dialect.oracle;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jul 3, 2010
 */
interface SQLTypeFactory {

    public abstract Struct createStruct(SDOGeometry geom, Connection conn) throws SQLException;

    public abstract Array createElemInfoArray(ElemInfo elemInfo, Connection conn) throws SQLException;

    public abstract Array createOrdinatesArray(Ordinates ordinates, Connection conn) throws SQLException;

}
