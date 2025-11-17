/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.temporal;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class AbstractRemappingH2Dialect extends H2Dialect {
	private final int overriddenSqlTypeCode;
	private final int overridingSqlTypeCode;

	public AbstractRemappingH2Dialect(Dialect baseDialect, int overriddenSqlTypeCode, int overridingSqlTypeCode) {
		super( baseDialect.getVersion() );
		this.overriddenSqlTypeCode = overriddenSqlTypeCode;
		this.overridingSqlTypeCode = overridingSqlTypeCode;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		typeContributions.getTypeConfiguration().getJdbcTypeRegistry().addDescriptor(
				overriddenSqlTypeCode,
				typeContributions.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor(
						overridingSqlTypeCode
				)
		);
	}

}
