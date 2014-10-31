/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.cfg.naming;

import java.io.Serializable;

import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;

import static org.hibernate.cfg.naming.LegacyNamingStrategyDelegate.LegacyNamingStrategyDelegateContext;

/**
 * @deprecated Needed as a transitory implementation until the deprecated NamingStrategy contract
 * can be removed.
 *
 * @author Gail Badner
 */
@Deprecated
public class LegacyNamingStrategyDelegator
		implements NamingStrategyDelegator, LegacyNamingStrategyDelegateContext, Serializable {
	public static final NamingStrategyDelegator DEFAULT_INSTANCE = new LegacyNamingStrategyDelegator();

	private final NamingStrategy namingStrategy;
	private final NamingStrategyDelegate hbmNamingStrategyDelegate;
	private final NamingStrategyDelegate jpaNamingStrategyDelegate;

	public LegacyNamingStrategyDelegator() {
		this( EJB3NamingStrategy.INSTANCE );
	}

	public LegacyNamingStrategyDelegator(NamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
		this.hbmNamingStrategyDelegate = new LegacyHbmNamingStrategyDelegate( this );
		this.jpaNamingStrategyDelegate = new LegacyJpaNamingStrategyDelegate( this );
	}

	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	@Override
	public NamingStrategyDelegate getNamingStrategyDelegate(boolean isHbm) {
		return isHbm ?
				hbmNamingStrategyDelegate :
				jpaNamingStrategyDelegate;
	}
}
