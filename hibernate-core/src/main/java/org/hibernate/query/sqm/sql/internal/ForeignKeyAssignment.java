package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayDeque;
import java.util.Deque;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmTreatedPath;
import org.hibernate.sql.ast.spi.creation.FromClauseAccess;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.translation.Clause;

import jakarta.annotation.Nullable;

import static org.hibernate.query.sqm.internal.SqmUtil.canUseForeignKey;

/// How an assignment, in the `SET` clause of an update, to a path through a to-one is
/// written.
///
/// An assignment to a to-one, to the key of its target, or to a part of that key writes
/// the corresponding part of the foreign key. When a read of the association joins the
/// target, because of `@NotFound`, `@SQLRestriction`, or `@SoftDelete`, the assignment
/// still does not join the target, but renders the foreign key, as [#resolve] does.
///
/// The unowned side of an association has no foreign key, so an update cannot assign it,
/// nor anything reached through it, as [#verifyAssignable] checks.
///
/// @param keyPart the part of the foreign key which is assigned
/// @param assignedPart the part which the path assigns, which is the to-one, or a part
///                     of the key of its target
/// @param ownerTableGroup the table group of the updated entity, which holds the foreign key
public record ForeignKeyAssignment(ModelPart keyPart, ModelPart assignedPart, TableGroup ownerTableGroup) {

	/// The part of a foreign key assigned by the given path, which is a to-one whose
	/// target is joined for a read, the key of its target, or a part of that key, or
	/// null if it is no such path. The to-one is the first one reached from the updated
	/// entity, perhaps through embeddables. The parts after it are parts of the key of
	/// its target, perhaps embeddables, or associations in them, as in
	/// `set o.target.id.reference = :ref`.
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
				return toOne.getSideNature() == ForeignKeyDescriptor.Nature.KEY
					&& !canUseForeignKey( toOne, creationState )
						? resolve( toOne, names, ownerTableGroup )
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

	private static @Nullable ForeignKeyAssignment resolve(
			ToOneAttributeMapping toOne,
			Deque<String> names,
			TableGroup ownerTableGroup) {
		if ( names.isEmpty() ) {
			return new ForeignKeyAssignment( toOne.getForeignKeyDescriptor().getKeyPart(), toOne, ownerTableGroup );
		}
		final String name = names.pop();
		ModelPart keyPart = toOne.findForeignKeySubPart( name, null );
		ModelPart assignedPart = toOne.findSubPart( name, null );
		while ( keyPart != null && assignedPart != null && !names.isEmpty() ) {
			// the parts after the to-one are embeddables of the key of its target, or
			// associations in them, and so are the corresponding parts of the foreign key
			final String next = names.pop();
			keyPart = ( (ModelPartContainer) keyPart ).findSubPart( next, null );
			assignedPart = ( (ModelPartContainer) assignedPart ).findSubPart( next, null );
		}
		return keyPart == null || assignedPart == null
				? null
				: new ForeignKeyAssignment( keyPart, assignedPart, ownerTableGroup );
	}

	/// Reject an assignment to the unowned side of an association, or to a part reached
	/// through it, perhaps via embeddables.
	///
	/// @param assigned the model part which the path is interpreted as
	///
	/// @throws SemanticException if the path is the unowned side of an association, or
	///                           is reached through it
	public static void verifyAssignable(SqmPath<?> path, ModelPart assigned, FromClauseAccess fromClauseAccess) {
		final var unowned = unownedAssociation( path, assigned, fromClauseAccess );
		if ( unowned != null ) {
			throw new SemanticException( "Cannot assign '" + path.getNavigablePath().getFullPath()
					+ "' since '" + unowned.getNavigableRole().getFullPath()
					+ "' is the unowned side of an association (assign the owning side instead)" );
		}
	}

	private static @Nullable ModelPart unownedAssociation(
			SqmPath<?> path,
			ModelPart assigned,
			FromClauseAccess fromClauseAccess) {
		if ( isUnownedAssociation( assigned ) ) {
			return assigned;
		}
		for ( var lhs = path.getLhs(); lhs != null; lhs = lhs.getLhs() ) {
			final var lhsTableGroup = fromClauseAccess.findTableGroup( lhs.getNavigablePath() );
			if ( lhsTableGroup != null && isUnownedAssociation( lhsTableGroup.getModelPart() ) ) {
				return lhsTableGroup.getModelPart();
			}
		}
		return null;
	}

	private static boolean isUnownedAssociation(ModelPart part) {
		return part instanceof ToOneAttributeMapping toOne
			&& toOne.getSideNature() == ForeignKeyDescriptor.Nature.TARGET;
	}
}
