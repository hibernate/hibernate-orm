/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class LiteralTests extends BaseSqmUnitTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		StandardDomainModel.GAMBIT.getDescriptor().applyDomainModel( metadataSources );
	}

	@Test
	public void testTimestampLiteral() {
		final SqmSelectStatement sqm = interpretSelect( "from EntityOfBasics e1 where e1.theTimestamp = {ts '2018-01-01T12:30:00'}" );
	}

	@Test
	public void testDateLiteral() {
		final SqmSelectStatement sqm = interpretSelect( "from EntityOfBasics e1 where e1.theDate = {d '2018-01-01'}" );
	}

	@Test
	public void testTimeLiteral() {
		final SqmSelectStatement sqm = interpretSelect( "from EntityOfBasics e1 where e1.theTime = {t '12:30:00'}" );
	}
}
