package org.hibernate.sql.results.graph.embeddable;

import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;

/**
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface EmbeddableValuedFetchable<J, M, R> extends EmbeddableValuedModelPart<J, M, R>, Fetchable<J, M, R> {
	@Override
	default SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.LEFT;
	}

	@Override
	default boolean isSimpleJoinPredicate(Predicate predicate) {
		return predicate == null;
	}
}
