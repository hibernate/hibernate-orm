/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.cfg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HSConfigurationTest {

	@Test
	public void testConfigureFailure() {
		HSConfiguration config = new HSConfiguration();
		config.configure("non-existing-file");
	}

	@Test
	public void testConfigureFile() {
		HSConfiguration config = new HSConfiguration();
		config.configure("test.cfg.xml");
		testResults(config);
	}


	private void testResults(HSConfiguration config) {
		assertEquals(
				"org.hibernate.spatial.postgis.PostgisDialect", config
				.getDefaultDialect()
		);
		assertEquals("FIXED", config.getPrecisionModel());
		assertEquals("5", config.getPrecisionModelScale());
	}

}
