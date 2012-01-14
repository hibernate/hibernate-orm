/*
 * $Id: GeometryCollectionConvertorTest.java 167 2010-03-11 22:26:10Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2010 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */

package org.hibernate.spatial.testing.dialects.sqlserver.convertors;

import org.hibernate.spatial.dialect.sqlserver.SqlServer2008SpatialDialect;
import org.hibernate.spatial.dialect.sqlserver.convertors.OpenGisType;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: maesenka
 * Date: Jan 24, 2010
 * Time: 5:33:19 PM
 * To change this template use File | Settings | File Templates.
 */
@RequiresDialect(value=SqlServer2008SpatialDialect.class)
public class GeometryCollectionConvertorTest extends AbstractConvertorTest {

	@BeforeClassOnce
	public void setUp() {
        super.beforeClass();
		doDecoding(OpenGisType.GEOMETRYCOLLECTION);
		doEncoding();
	}

	@Test
	public void test_encoding() {
		super.test_encoding();
	}

	@Test
	public void test_decoding() {
		super.test_decoding();
	}

    @AfterClassOnce
    public void afterClass(){
        super.afterClass();
    }

}
