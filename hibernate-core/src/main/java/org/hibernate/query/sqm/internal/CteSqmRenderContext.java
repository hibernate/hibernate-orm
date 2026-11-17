package org.hibernate.query.sqm.internal;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteTable;
import org.hibernate.query.sqm.tree.spi.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;

/** HQL names visible in a query with a WITH clause. */
public final class CteSqmRenderContext implements SqmRenderContext {
	private final SqmRenderContext parent;
	private final Map<SqmCteTable<?>, String> names = new IdentityHashMap<>();
	private final Set<String> visibleNames;

	public CteSqmRenderContext(SqmRenderContext parent, SqmCteContainer container) {
		this.parent = parent;
		visibleNames = parent instanceof CteSqmRenderContext outer
				? new HashSet<>( outer.visibleNames )
				: new HashSet<>();
		final var metamodel = container.nodeBuilder().getJpaMetamodel();
		for ( var cte : container.getCteStatements() ) {
			final String preferred = parent.resolveCteName( cte );
			String name = preferred;
			// HQL resolves entity names before CTEs. Also reserve names explicitly
			// declared later in this WITH clause when allocating a replacement.
			for ( int i = 1; visibleNames.contains( name ) || metamodel.getHqlEntityReference( name ) != null
					|| ( !name.equals( cte.getName() ) && container.getCteStatement( name ) != null ); i++ ) {
				name = preferred + '_' + i;
			}
			visibleNames.add( name );
			names.put( cte.getCteTable(), name );
		}
	}

	@Override
	public String resolveCteName(SqmCteStatement<?> cte) {
		final var name = names.get( cte.getCteTable() );
		return name == null ? parent.resolveCteName( cte ) : name;
	}

	@Override
	public String resolveAlias(SqmFrom<?, ?> from) {
		return parent.resolveAlias( from );
	}

	@Override
	public String resolveParameterName(JpaCriteriaParameter<?> parameter) {
		return parent.resolveParameterName( parameter );
	}
}
