package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayDeque;
import java.util.Deque;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmTreatedPath;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.translation.Clause;

import jakarta.annotation.Nullable;

import static org.hibernate.query.sqm.internal.SqmUtil.canUseForeignKey;

/// How an assignment, in the `SET` clause of an update, to a to-one is written.
///
/// An assignment to a to-one writes its foreign key. When a read of the association joins
/// the target, because of `@NotFound`, `@SQLRestriction`, or `@SoftDelete`, the assignment
/// still does not join the target, but renders the foreign key, as [#resolve] does.
///
/// @param keyPart the foreign key which is assigned
/// @param assignedPart the to-one which the path assigns
/// @param ownerTableGroup the table group of the updated entity, which holds the foreign key
public record ForeignKeyAssignment(ModelPart keyPart, ModelPart assignedPart, TableGroup ownerTableGroup) {

	/// The foreign key assigned by the given path, which is a to-one of the updated
	/// entity, perhaps reached through embeddables, whose target is joined for a read,
	/// or null if it is no such path.
	///
	/// The path is resolved from the mapping model, before any table group is created
	/// for it, so that the target is never joined.
	public static @Nullable ForeignKeyAssignment resolve(SqmPath<?> path, SqmToSqlAstConverter creationState) {
		if ( creationState.getCurrentClauseStack().getCurrent() != Clause.SET ) {
			return null;
		}
		// the names of the parts from the updated entity down to the given path
		final Deque<String> names = new ArrayDeque<>();
		var root = path;
		while ( root.getLhs() != null ) {
			if ( root instanceof SqmTreatedPath<?, ?> ) {
				return null;
			}
			names.push( root.getReferencedPathSource().getPathName() );
			root = root.getLhs();
		}
		final var ownerTableGroup = creationState.getFromClauseAccess().findTableGroup( root.getNavigablePath() );
		if ( ownerTableGroup == null ) {
			return null;
		}
		ModelPartContainer container = ownerTableGroup.getModelPart();
		while ( !names.isEmpty() ) {
			final var part = container.findSubPart( names.pop(), null );
			if ( part instanceof ToOneAttributeMapping toOne ) {
				return names.isEmpty()
					&& toOne.getSideNature() == ForeignKeyDescriptor.Nature.KEY
					&& !canUseForeignKey( toOne, creationState )
						? new ForeignKeyAssignment( toOne.getForeignKeyDescriptor().getKeyPart(), toOne, ownerTableGroup )
						: null;
			}
			else if ( part instanceof EmbeddableValuedModelPart embeddable ) {
				container = embeddable;
			}
			else {
				return null;
			}
		}
		return null;
	}
}
