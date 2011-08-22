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

    Struct createStruct(SDOGeometry geom, Connection conn) throws SQLException;

    Array createElemInfoArray(ElemInfo elemInfo, Connection conn) throws SQLException;

    Array createOrdinatesArray(Ordinates ordinates, Connection conn) throws SQLException;

}
