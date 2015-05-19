/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToOneType;
import org.hibernate.boot.model.JavaTypeDescriptor;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.DerivedValueSource;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.SingularAttributeNature;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceOneToOne;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Implementation for {@code <one-to-one/>} mappings
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
class SingularAttributeSourceOneToOneImpl
		extends AbstractToOneAttributeSourceImpl
		implements SingularAttributeSourceOneToOne {

	private final JaxbHbmOneToOneType oneToOneElement;
	private final HibernateTypeSourceImpl typeSource;

	private final String referencedTypeName;

	private final List<DerivedValueSource> formulaSources;

	private final AttributeRole attributeRole;
	private final AttributePath attributePath;

	private final FetchCharacteristicsSingularAssociationImpl fetchCharacteristics;

	private ToolingHintContext toolingHintContext;

	SingularAttributeSourceOneToOneImpl(
			MappingDocument mappingDocument,
			AttributeSourceContainer container,
			final JaxbHbmOneToOneType oneToOneElement,
			final String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		super( mappingDocument, naturalIdMutability );
		this.oneToOneElement = oneToOneElement;

		this.referencedTypeName = oneToOneElement.getClazz() != null
				? metadataBuildingContext().qualifyClassName( oneToOneElement.getClazz() )
				: oneToOneElement.getEntityName();
		final JavaTypeDescriptor referencedTypeDescriptor = new JavaTypeDescriptor() {
			@Override
			public String getName() {
				return referencedTypeName;
			}
		};
		this.typeSource = new HibernateTypeSourceImpl( referencedTypeDescriptor );

		if ( StringHelper.isNotEmpty( oneToOneElement.getFormulaAttribute() ) ) {
			formulaSources = Collections.singletonList(
					(DerivedValueSource) new FormulaImpl( mappingDocument, logicalTableName, oneToOneElement.getFormulaAttribute() )
			);
		}
		else if ( !oneToOneElement.getFormula().isEmpty() ) {
			this.formulaSources = CollectionHelper.arrayList( oneToOneElement.getFormula().size() );
			for ( String expression : oneToOneElement.getFormula() ) {
				formulaSources.add(
						new FormulaImpl( mappingDocument, logicalTableName, expression )
				);
			}
		}
		else {
			this.formulaSources = Collections.emptyList();
		}

		this.attributeRole = container.getAttributeRoleBase().append( oneToOneElement.getName() );
		this.attributePath = container.getAttributePathBase().append( oneToOneElement.getName() );

		this.fetchCharacteristics = FetchCharacteristicsSingularAssociationImpl.interpretOneToOne(
				mappingDocument.getMappingDefaults(),
				oneToOneElement.getFetch(),
				oneToOneElement.getOuterJoin(),
				oneToOneElement.getLazy(),
				oneToOneElement.isConstrained()
		);

		this.toolingHintContext = Helper.collectToolingHints(
				container.getToolingHintContext(),
				oneToOneElement
		);
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.ONE_TO_ONE;
	}

	@Override
	public String getName() {
		return oneToOneElement.getName();
	}

	@Override
	public String getXmlNodeName() {
		return oneToOneElement.getNode();
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
		return oneToOneElement.getAccess();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return true;
	}

	@Override
	public String getCascadeStyleName() {
		return oneToOneElement.getCascade();
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.ONE_TO_ONE;
	}

	@Override
	public Boolean isInsertable() {
		return false;
	}

	@Override
	public Boolean isUpdatable() {
		return false;
	}

	@Override
	public boolean isBytecodeLazy() {
		return false;
	}

	@Override
	public FetchCharacteristicsSingularAssociationImpl getFetchCharacteristics() {
		return fetchCharacteristics;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public String getReferencedEntityName() {
		return referencedTypeName;
	}

	@Override
	public boolean isUnique() {
		return true;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return oneToOneElement.getForeignKey();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return false;
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return oneToOneElement.isConstrained()  ? ForeignKeyDirection.FROM_PARENT : ForeignKeyDirection.TO_PARENT;
	}

	@Override
	public List<DerivedValueSource> getFormulaSources() {
		return formulaSources;
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}

	@Override
	public boolean isConstrained() {
		return oneToOneElement.isConstrained();
	}

	@Override
	public String getReferencedEntityAttributeName() {
		return oneToOneElement.getPropertyRef();
	}

	@Override
	public boolean isEmbedXml() {
		return oneToOneElement.isEmbedXml();
	}
}
