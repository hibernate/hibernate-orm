/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid;

import org.hibernate.type.descriptor.jdbc.UuidAsBinaryJdbcType;
import java.sql.Types;

/**
 * @author Jan Schatteman
 */
public class SybaseUuidAsVarbinaryJdbcType extends UuidAsBinaryJdbcType {
	@Override
	public int getDdlTypeCode() {
		return Types.VARBINARY;
	}
}
