/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

/**
 * Loader specialization for loading multiple {@linkplain Loadable loadable}
 * references by primary, foreign or natural key.
 *
 * @author Steve Ebersole
 */
public interface MultiKeyLoader extends Loader {
}
