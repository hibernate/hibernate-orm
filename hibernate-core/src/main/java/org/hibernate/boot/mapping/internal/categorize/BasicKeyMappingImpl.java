package org.hibernate.boot.mapping.internal.categorize;


/// Standard BasicKeyMapping impl
///
/// @since 9.0
/// @author Steve Ebersole
public record BasicKeyMappingImpl(AttributeMetadataImplementor attribute) implements BasicKeyMapping {
	@Override
	public AttributeMetadataImplementor getAttribute() {
		return attribute;
	}
}
