package org.hibernate.query.sqm.tree.spi.domain;

import java.util.Set;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * A plural-valued path selection produced by the plural collection functions
 * {@code elements()}, {@code values()}, {@code indices()}, and {@code keys()}.
 *
 * @author Gavin King
 */
public class SqmPluralPartSelectionPath<C> extends SqmPluralValuedSimplePath<C> {
	private final @jakarta.annotation.Nullable CollectionPart.Nature selectedPartNature;
	private final JavaType<C> javaType;
	private final SqmBindableType<C> selectionType;

	public SqmPluralPartSelectionPath(
			@Nonnull SqmPluralValuedSimplePath<C> pluralPath,
			@Nullable CollectionPart.Nature selectedPartNature) {
		this(
				pluralPath.getNavigablePath(),
				(SqmPluralPersistentAttribute<?, C, ?>) pluralPath.getModel(),
				pluralPath.getLhs(),
				pluralPath.getExplicitAlias(),
				pluralPath.nodeBuilder(),
				selectedPartNature
		);
	}

	@SuppressWarnings("unchecked")
	private SqmPluralPartSelectionPath(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPluralPersistentAttribute<?, C, ?> referencedNavigable,
			@Nullable SqmPath<?> lhs,
			@Nullable String explicitAlias,
			@Nonnull NodeBuilder nodeBuilder,
			@jakarta.annotation.Nullable CollectionPart.Nature selectedPartNature) {
		super( navigablePath, referencedNavigable, lhs, explicitAlias, nodeBuilder );
		this.selectedPartNature = selectedPartNature;
		this.javaType = selectedPartNature == null
				? referencedNavigable.getAttributeJavaType()
				: (JavaType<C>) nodeBuilder.getTypeConfiguration()
						.getJavaTypeRegistry()
						.resolveDescriptor( Set.class );
		this.selectionType = new PluralAttributeCollectionType<>( javaType );
	}

	public @Nullable CollectionPart.Nature getSelectedPartNature() {
		return selectedPartNature;
	}

	@Nonnull
	@Override
	public SqmPluralPartSelectionPath<C> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new SqmPluralPartSelectionPath<>(
						getNavigablePathCopy( lhsCopy ),
						(SqmPluralPersistentAttribute<?, C, ?>) getModel(),
						lhsCopy,
						getExplicitAlias(),
						nodeBuilder(),
						selectedPartNature
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public @Nonnull JavaType<C> getJavaTypeDescriptor() {
		return javaType;
	}

	@Override
	public @Nonnull JavaType<C> getNodeJavaType() {
		return javaType;
	}

	@Override
	public @Nonnull SqmBindableType<C> getExpressible() {
		return selectionType;
	}

	@Override
	public @Nonnull SqmBindableType<C> getNodeType() {
		return selectionType;
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		if ( selectedPartNature == null ) {
			super.appendHqlString( hql, context );
		}
		else {
			hql.append( selectedPartNature == CollectionPart.Nature.INDEX ? "indices(" : "elements(" );
			getLhs().appendHqlString( hql, context );
			hql.append( '.' ).append( getReferencedPathSource().getPathName() );
			hql.append( ')' );
		}
	}
}
