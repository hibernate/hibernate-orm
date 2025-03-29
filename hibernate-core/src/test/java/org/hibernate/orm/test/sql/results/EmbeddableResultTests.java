/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.results;

import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.graph.DomainResultGraphPrinter;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@SessionFactory( exportSchema = false )
public class EmbeddableResultTests extends AbstractResultTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		final SelectStatement interpretation = interpret(
				"select e from EntityOfComposites e join fetch e.component c join fetch c.nested",
				scope.getSessionFactory()
		);
		DomainResultGraphPrinter.logDomainResultGraph( interpretation.getDomainResultDescriptors() );
	}
}
