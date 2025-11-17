/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.source.internal.hbm.RelationalValueSourceHelper;

/**
 * @author Steve Ebersole
 */
public interface ColumnsAndFormulasSourceContainer {
	RelationalValueSourceHelper.ColumnsAndFormulasSource getColumnsAndFormulasSource();
}
