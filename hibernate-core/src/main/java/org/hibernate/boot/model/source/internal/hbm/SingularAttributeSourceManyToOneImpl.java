/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOnDeleteEnum;
import org.hibernate.boot.model.JavaTypeDescriptor;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.RelationalValueSourceContainer;
import org.hibernate.boot.model.source.spi.SingularAttributeNature;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceManyToOne;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Implementation for {@code <many-to-one/>} mappings
 *
 * @author Steve Ebersole
 */
class SingularAttributeSourceManyToOneImpl
		extends AbstractToOneAttributeSourceImpl
		implements SingularAttributeSourceManyToOne, RelationalValueSourceContainer {

	private final JaxbHbmManyToOneType manyToOneElement;
	private final HibernateTypeSourceImpl typeSource;

	private final String referencedTypeName;

	private final List<RelationalValueSource> relationalValueSources;

	private final AttributeRole attributeRole;
	private final AttributePath attributePath;

	private final ToolingHintContext toolingHintContext;

	private final FetchCharacteristicsSingularAssociationImpl fetchCharacteristics;

	SingularAttributeSourceManyToOneImpl(
			MappingDocument mappingDocument,
			AttributeSourceContainer container,
			final JaxbHbmManyToOneType manyToOneElement,
			final String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		super( mappingDocument, naturalIdMutability );
		this.manyToOneElement = manyToOneElement;

		this.referencedTypeName = manyToOneElement.getClazz() != null
				? mappingDocument.qualifyClassName( manyToOneElement.getClazz() )
				: manyToOneElement.getEntityName();

		final JavaTypeDescriptor referencedTypeDescriptor = new JavaTypeDescriptor() {
			@Override
			public String getName() {
				return referencedTypeName;
			}
		};
		this.typeSource = new HibernateTypeSourceImpl( referencedTypeDescriptor );

		this.relationalValueSources = RelationalValueSourceHelper.buildValueSources(
				mappingDocument,
				logicalTableName,
				new ManyToOneAttributeColumnsAndFormulasSource( manyToOneElement )
		);

		this.attributeRole = container.getAttributeRoleBase().append( manyToOneElement.getName() );
		this.attributePath = container.getAttributePathBase().append( manyToOneElement.getName() );

		this.fetchCharacteristics = FetchCharacteristicsSingularAssociationImpl.interpretManyToOne(
				mappingDocument.getMappingDefaults(),
				manyToOneElement.getFetch(),
				manyToOneElement.getOuterJoin(),
				manyToOneElement.getLazy()
		);

		this.toolingHintContext = Helper.collectToolingHints(
				container.getToolingHintContext(),
				manyToOneElement
		);
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.MANY_TO_ONE;
	}

	@Override
	public String getName() {
			return manyToOneElement.getName();
	}

	@Override
	public String getXmlNodeName() {
		return manyToOneElement.getNode();
	}

	@Override
	public AttributePath getAttributePath() {
		return attributePath;
	}

	@Override
	public AttributeRole getAttributeRole() {
		return attributeRole;
	}

	@Override
	public HibernateTypeSourceImpl getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return manyToOneElement.getAccess();
	}

	@Override
	public FetchCharacteristicsSingularAssociationImpl getFetchCharacteristics() {
		return fetchCharacteristics;
	}

	@Override
	public boolean isIgnoreNotFound() {
		return manyToOneElement.getNotFound() != null && "ignore".equalsIgnoreCase( manyToOneElement.getNotFound().value() );
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return manyToOneElement.isOptimisticLock();
	}

	@Override
	public String getCascadeStyleName() {
		return manyToOneElement.getCascade() == null
				? ""
				: manyToOneElement.getCascade();
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.MANY_TO_ONE;
	}

	@Override
	public Boolean isInsertable() {
		return manyToOneElement.isInsert();
	}

	@Override
	public Boolean isUpdatable() {
		return manyToOneElement.isUpdate();
	}

	@Override
	public boolean isBytecodeLazy() {
		return false;
	}

	@Override
	public String getReferencedEntityAttributeName() {
		return manyToOneElement.getPropertyRef();
	}

	@Override
	public String getReferencedEntityName() {
		return referencedTypeName;
	}

	@Override
	public Boolean isEmbedXml() {
		return manyToOneElement.isEmbedXml();
	}

	@Override
	public boolean isUnique() {
		return manyToOneElement.isUnique();
	}

	@Override
	public String getExplicitForeignKeyName() {
		return manyToOneElement.getForeignKey();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return JaxbHbmOnDeleteEnum.CASCADE.equals( manyToOneElement.getOnDelete() );
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.TO_PARENT;
	}

	@Override
	public List<RelationalValueSource> getRelationalValueSources() {
		return relationalValueSources;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return getNaturalIdMutability() == NaturalIdMutability.NOT_NATURAL_ID;
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}
}
