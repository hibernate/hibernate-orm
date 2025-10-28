/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.hibernate.cfg.BatchSettings.ORDER_INSERTS;
import static org.hibernate.cfg.BatchSettings.ORDER_UPDATES;
import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-16485")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = {
		InsertOrderingRCATest.WeightedCause.class,
		InsertOrderingRCATest.TimeManipulation.class,
		InsertOrderingRCATest.Symptom.class,
		InsertOrderingRCATest.SimpleCondition.class,
		InsertOrderingRCATest.RCATemplate.class,
		InsertOrderingRCATest.ParameterExpression.class,
		InsertOrderingRCATest.NumberedExpression.class,
		InsertOrderingRCATest.MathExpression.class,
		InsertOrderingRCATest.FieldExpression.class,
		InsertOrderingRCATest.Expression.class,
		InsertOrderingRCATest.ConstantExpression.class,
		InsertOrderingRCATest.ConditionAndExpression.class,
		InsertOrderingRCATest.ConditionalExpression.class,
		InsertOrderingRCATest.Condition.class,
		InsertOrderingRCATest.CompoundCondition.class,
		InsertOrderingRCATest.Cause.class,
		InsertOrderingRCATest.CalculationExpression.class,
		InsertOrderingRCATest.AlertCondition.class,
		InsertOrderingRCATest.BaseEntity.class
})
@SessionFactory
public class InsertOrderingRCATest implements ServiceRegistryProducer {
	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder.applySetting( ORDER_INSERTS, true )
				.applySetting( ORDER_UPDATES, true )
				.applySetting( STATEMENT_BATCH_SIZE, 50 )
				.applySetting( CONNECTION_PROVIDER, connectionProvider )
				.build();
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
		connectionProvider.stop();
	}

	@Test
	public void testBatching(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			connectionProvider.clear();
			for (RCATemplate template : DefaultTemplatesVault.getDefaultRCATemplates()) {
				session.persist(template);
			}
		});
	}

	@SuppressWarnings("unused")
	@Entity(name = "WeightedCause")
	@Table(name = "rca_weighted_cause")
	public static class WeightedCause extends BaseEntity {
		private Cause cause;
		private Integer weight;

		public WeightedCause() {
		}

		public WeightedCause(Cause cause, Integer weight) {
			this.cause = cause;
			this.weight = weight;
		}

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "cause_id")
		public Cause getCause() {
			return cause;
		}

		public void setCause(Cause cause) {
			this.cause = cause;
		}

		public Integer getWeight() {
			return weight;
		}

		public void setWeight(Integer weight) {
			this.weight = weight;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			WeightedCause cause1 = (WeightedCause) o;

			if ( !Objects.equals( cause, cause1.cause ) ) {
				return false;
			}
			return Objects.equals( weight, cause1.weight );
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (cause != null ? cause.hashCode() : 0);
			result = 31 * result + (weight != null ? weight.hashCode() : 0);
			return result;
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "TimeManipulation")
	@Table(name = "rca_time_manipulation")
	public static class TimeManipulation extends BaseEntity implements Serializable {
		private ManipulationType type;
		private MathOperator op;
		private Long time;
		private RelativeToType relativeToType;
		private String paramName;

		public TimeManipulation() {
		}

		public TimeManipulation(ManipulationType type, MathOperator op, Long time, RelativeToType relativeToType, String paramName) {
			this.type = type;
			this.op = op;
			this.time = time;
			this.relativeToType = relativeToType;
			this.paramName = paramName;
		}

		public ManipulationType getType() {
			return type;
		}

		public void setType(ManipulationType type) {
			this.type = type;
		}

		public MathOperator getOp() {
			return op;
		}

		public void setOp(MathOperator op) {
			this.op = op;
		}

		public Long getTime() {
			return time;
		}

		public void setTime(Long time) {
			this.time = time;
		}

		public RelativeToType getRelativeToType() {
			return relativeToType;
		}

		public void setRelativeToType(RelativeToType relativeToType) {
			this.relativeToType = relativeToType;
		}

		public String getParamName() {
			return paramName;
		}

		public void setParamName(String paramName) {
			this.paramName = paramName;
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "Symptom")
	@Table(name = "rca_symptom")
	public static class Symptom extends BaseEntity implements Serializable {
		private String name;
		private String objectType;
		private Condition condition;
		private String nodeTypeString;
		private String processTypeString;
		private String filterString;

		public Symptom() {
		}

		public Symptom(String name, String objectType, Condition condition) {
			this.name = name;
			this.objectType = objectType;
			this.condition = condition;
		}

		public String getObjectType() {
			return objectType;
		}

		public void setObjectType(String objectType) {
			this.objectType = objectType;
		}

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "condition_id")
		public Condition getCondition() {
			return condition;
		}

		public void setCondition(Condition condition) {
			this.condition = condition;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getNodeTypeString() {
			return nodeTypeString;
		}

		public void setNodeTypeString(String nodeTypeString) {
			this.nodeTypeString = nodeTypeString;
		}

		public String getProcessTypeString() {
			return processTypeString;
		}

		public void setProcessTypeString(String processTypeString) {
			this.processTypeString = processTypeString;
		}

		public String getFilterString() {
			return filterString;
		}

		public void setFilterString(String filterString) {
			this.filterString = filterString;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			Symptom symptom = (Symptom) o;

			if (condition != null ? !condition.equals(symptom.condition) : symptom.condition != null) {
				return false;
			}
			if ( !Objects.equals( name, symptom.name ) ) {
				return false;
			}
			return Objects.equals( objectType, symptom.objectType );
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (name != null ? name.hashCode() : 0);
			result = 31 * result + (objectType != null ? objectType.hashCode() : 0);
			result = 31 * result + (condition != null ? condition.hashCode() : 0);
			return result;
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "SimpleCondition")
	public static class SimpleCondition extends Condition {
		private Expression left;
		private Operator op;
		private Expression right;

		public SimpleCondition() {
		}

		public SimpleCondition(Expression left, Operator op, Expression right) {
			this.left = left;
			this.op = op;
			this.right = right;
		}

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinColumn(name = "left_expression_id")
		public Expression getLeft() {
			return left;
		}

		public void setLeft(Expression left) {
			this.left = left;
		}

		public Operator getOp() {
			return op;
		}

		public void setOp(Operator op) {
			this.op = op;
		}

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinColumn(name = "right_expression_id")
		public Expression getRight() {
			return right;
		}

		public void setRight(Expression right) {
			this.right = right;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			SimpleCondition that = (SimpleCondition) o;

			if (left != null ? !left.equals(that.left) : that.left != null) {
				return false;
			}
			if (op != that.op) {
				return false;
			}
			return Objects.equals( right, that.right );
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (left != null ? left.hashCode() : 0);
			result = 31 * result + (op != null ? op.hashCode() : 0);
			result = 31 * result + (right != null ? right.hashCode() : 0);
			return result;
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "RCATemplate")
	public static class RCATemplate extends BaseEntity {
		private String name;
		private Symptom symptom;
		private Set<WeightedCause> possibleCauses;

		public RCATemplate() {
		}

		public RCATemplate(String name, Symptom symptom, Set<WeightedCause> possibleCauses) {
			this.name = name;
			this.symptom = symptom;
			this.possibleCauses = possibleCauses;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "symptom_id")
		public Symptom getSymptom() {
			return symptom;

		}

		public void setSymptom(Symptom symptom) {
			this.symptom = symptom;
		}

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinTable(name = "template_cause", joinColumns = {
			@JoinColumn(name = "f_template_id")}, inverseJoinColumns = {
			@JoinColumn(name = "f_cause_id")})
		public Set<WeightedCause> getPossibleCauses() {
			return possibleCauses;
		}

		public void setPossibleCauses(Set<WeightedCause> possibleCauses) {
			this.possibleCauses = possibleCauses;
		}

	}
	@SuppressWarnings("unused")
	@Entity(name = "ParameterExpression")
	public static class ParameterExpression extends Expression {
		private String parameterName;
		private String defaultValue;


		public ParameterExpression() {
		}

		public ParameterExpression(String parameterName, String defaultValue, MetadataFieldType type) {
			this.parameterName = parameterName;
			this.defaultValue = defaultValue;
			this.type = type;
		}

		public String getParameterName() {
			return parameterName;
		}

		public void setParameterName(String parameterName) {
			this.parameterName = parameterName;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			ParameterExpression that = (ParameterExpression) o;

			if ( !Objects.equals( defaultValue, that.defaultValue ) ) {
				return false;
			}
			return Objects.equals( parameterName, that.parameterName );
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (parameterName != null ? parameterName.hashCode() : 0);
			result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
			return result;
		}
	}
	@Entity(name = "NumberedExpression")
	@Table(name = "rca_numbered_exception")
	public static class NumberedExpression extends BaseEntity implements Comparable<NumberedExpression> {
		private Long num;
		private Expression expression;

		public NumberedExpression() {
		}

		public NumberedExpression(Long num, Expression expression) {
			this.num = num;
			this.expression = expression;
		}

		public Long getNum() {
			return num;
		}

		public void setNum(Long num) {
			this.num = num;
		}

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinColumn(name = "num_expression_id")
		public Expression getExpression() {
			return expression;
		}

		public void setExpression(Expression expression) {
			this.expression = expression;
		}

		@Override
		public int compareTo(NumberedExpression other) {
			return (int) (this.num - other.num);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			NumberedExpression that = (NumberedExpression) o;

			if ( !Objects.equals( expression, that.expression ) ) {
				return false;
			}
			return Objects.equals( num, that.num );
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (num != null ? num.hashCode() : 0);
			result = 31 * result + (expression != null ? expression.hashCode() : 0);
			return result;
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "MathExpression")
	public static class MathExpression extends Expression {
		private Expression left;
		private Expression right;
		private MathOperator op;

		public MathExpression() {
		}

		public MathExpression(Expression left, Expression right, MathOperator op) {
			this.left = left;
			this.right = right;
			this.op = op;
		}

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinColumn(name = "left_expression_id")
		public Expression getLeft() {
			return left;
		}

		public void setLeft(Expression left) {
			this.left = left;
		}

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinColumn(name = "right_expression_id")
		public Expression getRight() {
			return right;
		}

		public void setRight(Expression right) {
			this.right = right;
		}

		public MathOperator getOp() {
			return op;
		}

		public void setOp(MathOperator op) {
			this.op = op;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			MathExpression that = (MathExpression) o;

			if (left != null ? !left.equals(that.left) : that.left != null) {
				return false;
			}
			if (op != that.op) {
				return false;
			}
			return Objects.equals( right, that.right );
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (left != null ? left.hashCode() : 0);
			result = 31 * result + (right != null ? right.hashCode() : 0);
			result = 31 * result + (op != null ? op.hashCode() : 0);
			return result;
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "FieldExpression")
	public static class FieldExpression extends Expression {
		private String objectType;
		private String name;

		public FieldExpression() {
		}

		public FieldExpression(String objectType, String name) {
			this.objectType = objectType;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getObjectType() {
			return objectType;
		}

		public void setObjectType(String objectType) {
			this.objectType = objectType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			FieldExpression that = (FieldExpression) o;

			if ( !Objects.equals( name, that.name ) ) {
				return false;
			}
			return Objects.equals( objectType, that.objectType );
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (objectType != null ? objectType.hashCode() : 0);
			result = 31 * result + (name != null ? name.hashCode() : 0);
			return result;
		}
	}
	@Entity(name = "Expression")
	@Table(name = "rca_expression")
	public static abstract class Expression extends BaseEntity implements Serializable {
		protected MetadataFieldType type;

		protected Expression() {
		}

		protected Expression(MetadataFieldType type) {
			this.type = type;
		}

		public MetadataFieldType getType() {
			return type;
		}

		public void setType(MetadataFieldType type) {
			this.type = type;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			Expression that = (Expression) o;

			return type == that.type;
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (type != null ? type.hashCode() : 0);
			return result;
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "ConstantExpression")
	public static class ConstantExpression extends Expression {
		private String value;

		public ConstantExpression() {
		}

		public ConstantExpression(String value, MetadataFieldType type) {
			super(type);
			this.value = value;
		}

		@Column(name = "val")
		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			ConstantExpression that = (ConstantExpression) o;

			return Objects.equals( value, that.value );
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (value != null ? value.hashCode() : 0);
			return result;
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "ConditionAndExpression")
	@Table(name = "rca_cond_and_expr")
	public static class ConditionAndExpression extends BaseEntity {
		private Condition condition;
		private Expression expression;

		public ConditionAndExpression() {
		}

		public ConditionAndExpression(Condition condition, Expression expression) {
			this.condition = condition;
			this.expression = expression;
		}

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "cond_cond_id")
		public Condition getCondition() {
			return condition;
		}

		public void setCondition(Condition condition) {
			this.condition = condition;
		}

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinColumn(name = "cond_expression_id")
		public Expression getExpression() {
			return expression;
		}

		public void setExpression(Expression expression) {
			this.expression = expression;
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "ConditionalExpression")
	public static class ConditionalExpression extends Expression {
		private Set<ConditionAndExpression> possibilities;

		public ConditionalExpression() {
		}

		public ConditionalExpression(MetadataFieldType type, Set<ConditionAndExpression> possibilities) {
			super(type);
			this.possibilities = possibilities;
		}

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinTable(name = "rca_cond_posssibility", joinColumns = {
			@JoinColumn(name = "f_cond_id")}, inverseJoinColumns = {
			@JoinColumn(name = "f_pos_id")})
		public Set<ConditionAndExpression> getPossibilities() {
			return possibilities;
		}

		public void setPossibilities(Set<ConditionAndExpression> possibilities) {
			this.possibilities = possibilities;
		}
	}
	@Entity(name = "Condition")
	@Table(name = "rca_condition")
	public static abstract class Condition extends BaseEntity implements Serializable {
	}
	@SuppressWarnings("unused")
	@Entity(name = "CompoundCondition")
	public static class CompoundCondition extends Condition {
		private Condition first;
		private Condition second;
		private LogicalOperator op;

		public CompoundCondition() {
		}

		public CompoundCondition(Condition first, Condition second) {
			this.first = first;
			this.second = second;
			this.op = LogicalOperator.AND;
		}

		public CompoundCondition(Condition first, Condition second, LogicalOperator op) {
			this.first = first;
			this.second = second;
			this.op = op;
		}

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "first_id")
		public Condition getFirst() {
			return first;
		}

		public void setFirst(Condition first) {
			this.first = first;
		}

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "second_id")
		public Condition getSecond() {
			return second;
		}

		public void setSecond(Condition second) {
			this.second = second;
		}

		public LogicalOperator getOp() {
			return op;
		}

		public void setOp(LogicalOperator op) {
			this.op = op;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			CompoundCondition that = (CompoundCondition) o;

			if (first != null ? !first.equals(that.first) : that.first != null) {
				return false;
			}
			if (op != that.op) {
				return false;
			}
			return Objects.equals( second, that.second );
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (first != null ? first.hashCode() : 0);
			result = 31 * result + (second != null ? second.hashCode() : 0);
			result = 31 * result + (op != null ? op.hashCode() : 0);
			return result;
		}
	}
	@SuppressWarnings("unused")
	@Entity
	@Table(name = "rca_cause")
	public static class Cause extends BaseEntity implements Serializable {
		private String name;
		private String nodeType;
		private MappingRelationType relationType;
		private Set<Condition> fetchConditions;
		private Condition condition;
		private Condition auxCondition;
		private String messageTemplate;
		private String generalMessageTemplate;
		private DataManipulationFunction function;
		private Set<NumberedExpression> messageTemplateParams;
		private Set<NumberedExpression> generalMessageTemplateParams;
		private TimeManipulation startTimeManipulation;
		private TimeManipulation endTimeManipulation;
		private String plugin;

		public Cause() {
		}

		public Cause(String name, String nodeType, MappingRelationType relationType, Set<Condition> fetchConditions,
					Condition condition, String messageTemplate, DataManipulationFunction function, Set<NumberedExpression> messageTemplateParams, String plugin) {
			this.name = name;
			this.nodeType = nodeType;
			this.relationType = relationType;
			this.fetchConditions = fetchConditions;
			this.condition = condition;
			this.messageTemplate = messageTemplate;
			this.function = function;
			this.messageTemplateParams = messageTemplateParams;
			this.plugin = plugin;
		}

		public String getNodeType() {
			return nodeType;
		}

		public void setNodeType(String nodeType) {
			this.nodeType = nodeType;
		}

		public String getPlugin() {
			return plugin;
		}

		public void setPlugin(String plugin) {
			this.plugin = plugin;
		}

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "condition_id")
		public Condition getCondition() {
			return condition;
		}

		public void setCondition(Condition condition) {
			this.condition = condition;
		}

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "aux_condition_id")
		public Condition getAuxCondition() {
			return auxCondition;
		}

		public void setAuxCondition(Condition auxCondition) {
			this.auxCondition = auxCondition;
		}

		public String getMessageTemplate() {
			return messageTemplate;
		}

		public void setMessageTemplate(String messageTemplate) {
			this.messageTemplate = messageTemplate;
		}

		public String getGeneralMessageTemplate() {
			return generalMessageTemplate;
		}

		public void setGeneralMessageTemplate(String generalMessageTemplate) {
			this.generalMessageTemplate = generalMessageTemplate;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Column(name = "functionName")
		public DataManipulationFunction getFunction() {
			return function;
		}

		public void setFunction(DataManipulationFunction function) {
			this.function = function;
		}

		public MappingRelationType getRelationType() {
			return relationType;
		}

		public void setRelationType(MappingRelationType relationType) {
			this.relationType = relationType;
		}

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinTable(name = "rca_cause_param", joinColumns = {
			@JoinColumn(name = "f_cause_id")}, inverseJoinColumns = {
			@JoinColumn(name = "f_param_id")})
		public Set<NumberedExpression> getMessageTemplateParams() {
			return messageTemplateParams;
		}

		public void setMessageTemplateParams(Set<NumberedExpression> messageTemplateParams) {
			this.messageTemplateParams = messageTemplateParams;
		}

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinTable(name = "rca_cause_general_param", joinColumns = {
			@JoinColumn(name = "f_cause_id")}, inverseJoinColumns = {
			@JoinColumn(name = "f_param_id")})
		public Set<NumberedExpression> getGeneralMessageTemplateParams() {
			return generalMessageTemplateParams;
		}

		public void setGeneralMessageTemplateParams(Set<NumberedExpression> generalMessageTemplateParams) {
			this.generalMessageTemplateParams = generalMessageTemplateParams;
		}

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinTable(name = "rca_cause_fetch", joinColumns = {
			@JoinColumn(name = "f_cause_id")}, inverseJoinColumns = {
			@JoinColumn(name = "f_fetch_id")})
		public Set<Condition> getFetchConditions() {
			return fetchConditions;
		}

		public void setFetchConditions(Set<Condition> fetchConditions) {
			this.fetchConditions = fetchConditions;
		}

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "start_time_manip_id")
		public TimeManipulation getStartTimeManipulation() {
			return startTimeManipulation;
		}

		public void setStartTimeManipulation(TimeManipulation startTimeManipulation) {
			this.startTimeManipulation = startTimeManipulation;
		}

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "end_time_manip_id")
		public TimeManipulation getEndTimeManipulation() {
			return endTimeManipulation;
		}

		public void setEndTimeManipulation(TimeManipulation endTimeManipulation) {
			this.endTimeManipulation = endTimeManipulation;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			Cause cause = (Cause) o;

			if (function != cause.function) {
				return false;
			}
			if (messageTemplate != null ? !messageTemplate.equals(cause.messageTemplate) : cause.messageTemplate != null) {
				return false;
			}
			if (name != null ? !name.equals(cause.name) : cause.name != null) {
				return false;
			}
			if ( !Objects.equals( nodeType, cause.nodeType ) ) {
				return false;
			}
			return relationType == cause.relationType;
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (name != null ? name.hashCode() : 0);
			result = 31 * result + (nodeType != null ? nodeType.hashCode() : 0);
			result = 31 * result + (relationType != null ? relationType.hashCode() : 0);
			result = 31 * result + (messageTemplate != null ? messageTemplate.hashCode() : 0);
			result = 31 * result + (function != null ? function.hashCode() : 0);
			return result;
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "CalculationExpression")
	public static class CalculationExpression extends Expression {
		private DataManipulationFunction function;
		private String objectType;
		private String fieldName;

		public CalculationExpression() {
		}

		public CalculationExpression(DataManipulationFunction function, String objectType, String fieldName) {
			this.function = function;
			this.objectType = objectType;
			this.fieldName = fieldName;
		}

		@Column(name = "functionName")
		public DataManipulationFunction getFunction() {
			return function;
		}

		public void setFunction(DataManipulationFunction function) {
			this.function = function;
		}

		public String getObjectType() {
			return objectType;
		}

		public void setObjectType(String objectType) {
			this.objectType = objectType;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			CalculationExpression that = (CalculationExpression) o;

			if (fieldName != null ? !fieldName.equals(that.fieldName) : that.fieldName != null) {
				return false;
			}
			if (function != that.function) {
				return false;
			}
			return Objects.equals( objectType, that.objectType );
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (function != null ? function.hashCode() : 0);
			result = 31 * result + (objectType != null ? objectType.hashCode() : 0);
			result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
			return result;
		}
	}
	@SuppressWarnings("unused")
	@Entity(name = "AlertCondition")
	public static class AlertCondition extends Condition {
		private String ruleName;

		public AlertCondition() {
		}

		public String getRuleName() {
			return ruleName;
		}

		public void setRuleName(String ruleName) {
			this.ruleName = ruleName;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}

			AlertCondition that = (AlertCondition) o;

			return Objects.equals( ruleName, that.ruleName );
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (ruleName != null ? ruleName.hashCode() : 0);
			return result;
		}
	}
	@MappedSuperclass
	public static abstract class BaseEntity implements Serializable {
		private Long id;

		@Id
		@Column(name = "f_id")
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			BaseEntity that = (BaseEntity) o;

			// noinspection SimplifiableIfStatement
			if (id == null || that.id == null) {
				return false; // null != everything (including null)
			}
			return id.equals(that.id);
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}

	}

	public enum RelativeToType {
		START,
		END
	}

	@SuppressWarnings({"FieldMayBeFinal", "NonFinalFieldInEnum"})
	public enum Operator {
		EQUALS(" = "),
		NOT_EQUALS(" != "),
		GREATER_THAN(" > "),
		SMALLER_THAN(" < "),
		CONTAINS(" contains "),
		BEGINS_WITH(" begins with "),
		ENDS_WITH(" ends with ");

		private String readable;

		Operator(String readable) {
			this.readable = readable;
		}

		@Override
		public String toString() {
			return readable;
		}
	}

	public enum MathOperator {
		ADD,
		SUBTRACT,
		MULTIPLY,
		DIVIDE,
		MAX;

		@Override
		public String toString() {
			return switch ( this ) {
				case ADD -> "+";
				case SUBTRACT -> "-";
				case MULTIPLY -> "*";
				case DIVIDE -> "/";
				case MAX -> "max";
			};
		}


	}

	public enum ManipulationType {
		MOVE_IT_BY_ABSOLUTE_TIME,
		MOVE_IT_RELATIVE_TO_OTHER_TIME,
		ABSOLUTE
	}

	public enum LogicalOperator {
		AND,
		OR
	}

	public enum MetadataFieldType {
		BOOLEAN, STRING, NULL, NUMERIC
	}

	public enum DataManipulationFunction {
		AVG,
		MAX,
		MIN,
		COUNT,
		SUM,
		EXISTS,
		LAST,
		FIRST,
		INCREASED_BY
	}

	public enum MappingRelationType {
		JOB_BACKING_UP_HOST, JOB_USES_BACKUPCLIENT, JOB_USES_BACKUP_POOL, JOB_USES_MEDIA_SERVER, RECOVERPOINT_CG_COPY_ACTIVE_RPA, JOB_USES_TAPE_DRIVE
	}

	@SuppressWarnings("unused")
	public static class DefaultTemplatesVault {

		public static final String NO_AGENT_RAN_ON_CLIENT = "No agent ran on client";
		public static final String BACKUP_AGENT_IS_DOWN_ON_BACKUP_CLIENT = "Backup agent is down on backup client";
		public static final String BACKUP_FAILED_SYMPTOM_NAME = "Backup failed";
		public static final String CG_COPY_RPO_VIOLATION_SYMPTOM_NAME = "CG Copy RPO Violation";
		public static final String PACKET_LOSS = "Packet Loss";

		private static RCATemplate buildBackupFailedTemplate() {
			Symptom symptom = new Symptom(BACKUP_FAILED_SYMPTOM_NAME, "Backupjob",
				new SimpleCondition(new FieldExpression("Backupjob", "status"), Operator.EQUALS, new ConstantExpression("failed", MetadataFieldType.STRING)));
			symptom.setFilterString("status = 'failed'");
			symptom.setNodeTypeString("clients");
			symptom.setProcessTypeString("backup jobs");

			// high client cpu                                                                                                               '
			WeightedCause cause1 = buildHighClientCpu();

			// high storage node cpu
			WeightedCause cause2 = buildHighStorageNodeCpu();

			// no space left on device
			WeightedCause cause3 = buildNoSpaceOnPool();

			// no space left on data domain
			WeightedCause cause4 = buildNoSpaceOnDD();

			// no agent ran on client
			WeightedCause cause5 = buildNoAgentOnClient();

			//  no agent ran on client
			WeightedCause cause6 = buildNoAgentOnClient2();

			// errors on tape
			WeightedCause errorsOnTapeCause = buildErrorsOnTape();

			//  switch port for ethernet
			WeightedCause cause7 = buildPortSwitchForEthernet();

			//  switch port for fibre channel
			WeightedCause cause8 = buildPortSwitchForFibreChannel();

			//  port settings for ethernet
			WeightedCause cause9 = buildDifferentPortSettings();

			//  no space on client
			WeightedCause noSpaceOnClientCause = buildNoSpaceOnClient();

			// todo -all other causes
			Set<WeightedCause> causes = new HashSet<>();
			causes.add(cause1);
			causes.add(cause2);
			causes.add(cause3);
			causes.add(cause4);
			causes.add(cause5);
			causes.add(cause6);
			//causes.add(errorsOnTapeCause);
			causes.add(cause7);
			causes.add(cause8);
			causes.add(cause9);
			causes.add(noSpaceOnClientCause);
			return new RCATemplate("Backup failed RCA template", symptom, causes);
		}

		private static WeightedCause buildNoSpaceOnClient() {
			Expression clientName = new FieldExpression("Host", "name");
			NumberedExpression nameFirst = new NumberedExpression(1L, clientName);

			Expression mountpointName = new FieldExpression("FilesystemConfig", "mountpoint");
			NumberedExpression mountpointSecond = new NumberedExpression(2L, mountpointName);

			Expression leftSpace = new MathExpression(new FieldExpression("FilesystemConfig", "totalSpace"),
				new FieldExpression("FilesystemStatus", "usedSpace"),
				MathOperator.SUBTRACT);
			ParameterExpression leftSpaceOnClient = new ParameterExpression("leftSpaceOnClient", "50", MetadataFieldType.NUMERIC);
			NumberedExpression spaceThird = new NumberedExpression(3L, leftSpace);


			Set<NumberedExpression> messageArgs = new HashSet<>();
			messageArgs.add(nameFirst);
			messageArgs.add(mountpointSecond);
			messageArgs.add(spaceThird);

			Condition fetchCondition1 = new CompoundCondition(
				new SimpleCondition(new FieldExpression("FilesystemStatus", "mountpoint"), Operator.EQUALS, new ConstantExpression("C:", MetadataFieldType.STRING)),
				new SimpleCondition(new FieldExpression("FilesystemStatus", "agentId"), Operator.EQUALS, new FieldExpression("Host", "id"))
			);
			Condition fetchCondition2 = new CompoundCondition(
				new SimpleCondition(new FieldExpression("FilesystemConfig", "mountpoint"), Operator.EQUALS, new ConstantExpression("C:", MetadataFieldType.STRING)),
				new SimpleCondition(new FieldExpression("FilesystemConfig", "agentId"), Operator.EQUALS, new FieldExpression("Host", "id"))
			);

			Set<Condition> fetchConditions = new HashSet<>();
			fetchConditions.add(fetchCondition1);
			fetchConditions.add(fetchCondition2);

			SimpleCondition condition = new SimpleCondition(leftSpace, Operator.SMALLER_THAN, leftSpaceOnClient);

			Cause noSpaceOnClient = new Cause("No space on Client", "Host", MappingRelationType.JOB_BACKING_UP_HOST,
				fetchConditions, condition, "There is a lack of free space in {param}:{param}. The current free space is {param} MB", DataManipulationFunction.LAST, messageArgs, null);

			return new WeightedCause(noSpaceOnClient, 95);
		}

		private static WeightedCause buildErrorsOnTape() {

			Expression tapeDriveNameExp = new FieldExpression("TapeDrive", "name");
			NumberedExpression tapeDriveName = new NumberedExpression(1L, tapeDriveNameExp);

			Expression errorsExp = new FieldExpression("TapedriveStatus", "correctedReadErrors");

			Set<NumberedExpression> messageArgs = new HashSet<>();
			messageArgs.add(tapeDriveName);

			Condition fetchCondition = new SimpleCondition(new FieldExpression("TapedriveStatus", "nodeId"),
				Operator.EQUALS, new FieldExpression("TapeDrive", "id"));
			Set<Condition> fetchConditions = new HashSet<>();
			fetchConditions.add(fetchCondition);

			ConstantExpression zero = new ConstantExpression("0", MetadataFieldType.NUMERIC);

			Cause errorsOnTape = new Cause("Errors on tape", "TapeDrive", MappingRelationType.JOB_USES_TAPE_DRIVE, fetchConditions,
				new CompoundCondition(
					new CompoundCondition(
						new CompoundCondition(
							new SimpleCondition(new FieldExpression("TapedriveStatus", "correctedReadErrors"), Operator.GREATER_THAN, zero),
							new SimpleCondition(new FieldExpression("TapedriveStatus", "correctedWriteErrors"), Operator.GREATER_THAN, zero),
							LogicalOperator.OR),
						new SimpleCondition(new FieldExpression("TapedriveStatus", "uncorrectedReadErrors"), Operator.GREATER_THAN, zero),
						LogicalOperator.OR),
					new SimpleCondition(new FieldExpression("TapedriveStatus", "uncorrectedWriteErrors"), Operator.GREATER_THAN, zero),
					LogicalOperator.OR),
				"Tape drive {param} had errors", DataManipulationFunction.INCREASED_BY, messageArgs, null);
			return new WeightedCause(errorsOnTape, 70);
		}

		private static WeightedCause buildNoSpaceOnDD() {
			Expression ddName = new FieldExpression("DataDomain", "name");
			NumberedExpression ddFirst = new NumberedExpression(1L, ddName);


			Expression ddUsage = new MathExpression(new MathExpression(
				new FieldExpression("FilesystemStatus", "usedSpace"),
				new FieldExpression("FilesystemConfig", "totalSpace"),
				MathOperator.DIVIDE
			),
				new ConstantExpression("100", MetadataFieldType.NUMERIC), MathOperator.MULTIPLY);
			NumberedExpression ddSecond = new NumberedExpression(2L, ddUsage);
			ParameterExpression ddUsageThreshold = new ParameterExpression("ddUsageThreshold", "80", MetadataFieldType.NUMERIC);
			NumberedExpression ddThird = new NumberedExpression(3L, ddUsageThreshold);
			Set<NumberedExpression> messageArgs4 = new HashSet<>();
			messageArgs4.add(ddFirst);
			messageArgs4.add(ddSecond);
			messageArgs4.add(ddThird);

			Condition fetchCondition4 = new CompoundCondition(
				new SimpleCondition(new FieldExpression("FilesystemStatus", "mountpoint"), Operator.EQUALS, new ConstantExpression("Data", MetadataFieldType.STRING)),
				new SimpleCondition(new FieldExpression("FilesystemStatus", "agentId"), Operator.EQUALS, new FieldExpression("DataDomain", "id"))
			);
			Condition fetchCondition5 = new CompoundCondition(
				new SimpleCondition(new FieldExpression("FilesystemConfig", "mountpoint"), Operator.EQUALS, new ConstantExpression("Data", MetadataFieldType.STRING)),
				new SimpleCondition(new FieldExpression("FilesystemConfig", "agentId"), Operator.EQUALS, new FieldExpression("DataDomain", "id"))
			);
			Set<Condition> fetchConditions4 = new HashSet<>();
			fetchConditions4.add(fetchCondition4);
			fetchConditions4.add(fetchCondition5);

			Cause noSpaceOnDD = new Cause("No space on data domain", "DataDomain", MappingRelationType.JOB_USES_BACKUPCLIENT,
				fetchConditions4, new SimpleCondition(ddUsage, Operator.GREATER_THAN, ddUsageThreshold),
				"The capacity utilization for DataDomain {param} is {param}% which is the above threshold {param}%", DataManipulationFunction.LAST, messageArgs4, null);

			noSpaceOnDD.setGeneralMessageTemplate("The capacity utilization for DataDomain {param} is the above threshold {param}%");

			Set<NumberedExpression> generalMessageParams = new HashSet<>();
			generalMessageParams.add(ddFirst);
			NumberedExpression ddThirdWhichIsNowSecond = new NumberedExpression(2L, new ParameterExpression("ddUsageThreshold", "80", MetadataFieldType.NUMERIC));
			generalMessageParams.add(ddThirdWhichIsNowSecond);
			noSpaceOnDD.setGeneralMessageTemplateParams(generalMessageParams);

			return new WeightedCause(noSpaceOnDD, 90);
		}

		private static WeightedCause buildNoSpaceOnPool() {
			Expression poolName = new FieldExpression("BackupPool", "name");
			NumberedExpression deviceFirst = new NumberedExpression(1L, poolName);
			Expression numOfEmptyVolumes = new CalculationExpression(DataManipulationFunction.COUNT, "VolumeStatus", "id");
			NumberedExpression deviceSecond = new NumberedExpression(2L, numOfEmptyVolumes);
			Set<NumberedExpression> messageArgs3 = new HashSet<>();
			messageArgs3.add(deviceFirst);
			messageArgs3.add(deviceSecond);

			Condition fetchCondition3 = new CompoundCondition(
				new SimpleCondition(new FieldExpression("VolumeStatus", "pool"), Operator.EQUALS, new FieldExpression("BackupPool", "name")),
				new SimpleCondition(new FieldExpression("VolumeStatus", "agentId"), Operator.EQUALS, new FieldExpression("Backupjob", "agentId"))
			);
			Set<Condition> fetchConditions3 = new HashSet<>();
			fetchConditions3.add(fetchCondition3);

			ConstantExpression threshold = new ConstantExpression("5", MetadataFieldType.NUMERIC);
			Cause noSpaceOnDevice = new Cause("No space left on device", "BackupPool", MappingRelationType.JOB_USES_BACKUP_POOL,
				fetchConditions3,
				new SimpleCondition(
					new FieldExpression("VolumeStatus", "state"),
					Operator.SMALLER_THAN,
					threshold),
				"Pool {param} has only {param} empty volumes left", DataManipulationFunction.COUNT, messageArgs3, null);
			noSpaceOnDevice.setAuxCondition(new SimpleCondition(new FieldExpression("VolumeStatus", "state"),
				Operator.EQUALS, new ConstantExpression("Empty", MetadataFieldType.STRING)));

			noSpaceOnDevice.setGeneralMessageTemplate("Pool {param} has less than {param} empty volumes left");
			Set<NumberedExpression> generalMessageParams = new HashSet<>();
			generalMessageParams.add(deviceFirst);
			generalMessageParams.add(new NumberedExpression(2L, threshold));
			noSpaceOnDevice.setGeneralMessageTemplateParams(generalMessageParams);

			return new WeightedCause(noSpaceOnDevice, 80);
		}

		private static WeightedCause buildHighStorageNodeCpu() {
			Expression storageName = new FieldExpression("Host", "name");
			NumberedExpression storageFirst = new NumberedExpression(1L, storageName);
			Expression avgCpuStorage = new CalculationExpression(DataManipulationFunction.AVG, "HostStatus", "cpuUsed");
			NumberedExpression storageSecond = new NumberedExpression(2L, avgCpuStorage);
			Set<NumberedExpression> messageArgs2 = new HashSet<>();
			messageArgs2.add(storageFirst);
			messageArgs2.add(storageSecond);

			Condition fetchCondition2 = new SimpleCondition(new FieldExpression("HostStatus", "nodeId"),
				Operator.EQUALS, new FieldExpression("Host", "id"));
			Set<Condition> fetchConditions2 = new HashSet<>();
			fetchConditions2.add(fetchCondition2);

			ParameterExpression storageNodeHighCPUThreshold = new ParameterExpression("storageNodeHighCPUThreshold", "95", MetadataFieldType.NUMERIC);
			Cause highCpuOnStorageNode = new Cause("High storage node CPU", "Host", MappingRelationType.JOB_USES_MEDIA_SERVER,
				fetchConditions2,
				new SimpleCondition(new FieldExpression("HostStatus", "cpuUsed"), Operator.GREATER_THAN,
					storageNodeHighCPUThreshold), "Storage node {param} CPU was {param}", DataManipulationFunction.AVG,
				messageArgs2, null);
			TimeManipulation startTimeManipulation = new TimeManipulation(ManipulationType.MOVE_IT_RELATIVE_TO_OTHER_TIME,
				MathOperator.SUBTRACT, 3600L, RelativeToType.END, "job_cpu_offset");
			highCpuOnStorageNode.setStartTimeManipulation(startTimeManipulation);

			highCpuOnStorageNode.setGeneralMessageTemplate("Storage node {param} CPU was higher than {param}");
			Set<NumberedExpression> generalMessageParams = new HashSet<>();
			generalMessageParams.add(storageFirst);
			generalMessageParams.add(new NumberedExpression(2L, storageNodeHighCPUThreshold));
			highCpuOnStorageNode.setGeneralMessageTemplateParams(generalMessageParams);

			return new WeightedCause(highCpuOnStorageNode, 60);
		}

		private static WeightedCause buildHighClientCpu() {
			Expression clientName = new FieldExpression("Host", "name");
			NumberedExpression clientFirst = new NumberedExpression(1L, clientName);
			Expression avgCpu = new CalculationExpression(DataManipulationFunction.AVG, "HostStatus", "cpuUsed");
			NumberedExpression clientSecond = new NumberedExpression(2L, avgCpu);
			Set<NumberedExpression> messageArgs1 = new HashSet<>();
			messageArgs1.add(clientFirst);
			messageArgs1.add(clientSecond);

			Condition fetchCondition = new SimpleCondition(new FieldExpression("HostStatus", "nodeId"),
				Operator.EQUALS, new FieldExpression("Host", "id"));
			Set<Condition> fetchConditions = new HashSet<>();
			fetchConditions.add(fetchCondition);


			ParameterExpression clientHighCPUThreshold = new ParameterExpression("clientHighCPUThreshold", "95", MetadataFieldType.NUMERIC);
			Cause highCpuOnClient = new Cause("High client CPU", "Host", MappingRelationType.JOB_BACKING_UP_HOST,
				new HashSet<>(fetchConditions),
				new SimpleCondition(new FieldExpression("HostStatus", "cpuUsed"), Operator.GREATER_THAN,
					clientHighCPUThreshold), "Client {param} CPU was {param}", DataManipulationFunction.AVG,
				messageArgs1, null);

			TimeManipulation startTimeManipulation = new TimeManipulation(ManipulationType.MOVE_IT_RELATIVE_TO_OTHER_TIME,
				MathOperator.SUBTRACT, 3600L, RelativeToType.END, "job_cpu_offset");
			highCpuOnClient.setStartTimeManipulation(startTimeManipulation);

			highCpuOnClient.setGeneralMessageTemplate("Client {param} CPU was higher than {param}");
			Set<NumberedExpression> generalMessageParams = new HashSet<>();
			generalMessageParams.add(clientFirst);
			generalMessageParams.add(new NumberedExpression(2L, clientHighCPUThreshold));
			highCpuOnClient.setGeneralMessageTemplateParams(generalMessageParams);

			return new WeightedCause(highCpuOnClient, 50);
		}

		private static WeightedCause buildNoAgentOnClient() {
			Expression clientName = new FieldExpression("Host", "name");
			NumberedExpression clientFirst = new NumberedExpression(1L, clientName);
			Set<NumberedExpression> messageArgs1 = new HashSet<>();
			messageArgs1.add(clientFirst);

			Condition fetchHostStatus = new SimpleCondition(new FieldExpression("HostStatus", "nodeId"),
				Operator.EQUALS, new FieldExpression("Host", "id"));

			ConditionAndExpression netbackup = new ConditionAndExpression(
				new SimpleCondition(
					new FieldExpression("Backupjob", "module"),
					Operator.EQUALS,
					new ConstantExpression("NetBackupModule", MetadataFieldType.STRING)
				),
				new ConstantExpression("bpinetd", MetadataFieldType.STRING)
			);

			ConditionAndExpression networker = new ConditionAndExpression(
				new SimpleCondition(
					new FieldExpression("Backupjob", "module"),
					Operator.EQUALS,
					new ConstantExpression("NetWorkerModule", MetadataFieldType.STRING)
				),
				new ConstantExpression("nsrexecd", MetadataFieldType.STRING)
			);

			ConditionAndExpression backupExec = new ConditionAndExpression(
				new SimpleCondition(
					new FieldExpression("Backupjob", "module"),
					Operator.EQUALS,
					new ConstantExpression("BackupExecModule", MetadataFieldType.STRING)
				),
				new ConstantExpression("beremote", MetadataFieldType.STRING)
			);

			ConditionAndExpression dataProtector = new ConditionAndExpression(
				new SimpleCondition(
					new FieldExpression("Backupjob", "module"),
					Operator.EQUALS,
					new ConstantExpression("DataProtectorModule", MetadataFieldType.STRING)
				),
				new ConstantExpression("omniinet", MetadataFieldType.STRING)
			);

			ConditionAndExpression tsm = new ConditionAndExpression(
				new SimpleCondition(
					new FieldExpression("Backupjob", "module"),
					Operator.EQUALS,
					new ConstantExpression("TSMModule", MetadataFieldType.STRING)
				),
				new ConstantExpression("dsmcsvc", MetadataFieldType.STRING)
			);

			ConditionAndExpression arcServe = new ConditionAndExpression(
				new SimpleCondition(
					new FieldExpression("Backupjob", "module"),
					Operator.EQUALS,
					new ConstantExpression("ArcserveModule", MetadataFieldType.STRING)
				),
				new ConstantExpression("UnivAgent", MetadataFieldType.STRING)
			);

			ConditionAndExpression commVault = new ConditionAndExpression(
				new SimpleCondition(
					new FieldExpression("Backupjob", "module"),
					Operator.EQUALS,
					new ConstantExpression("CommvaultModule", MetadataFieldType.STRING)
				),
				new ConstantExpression("evmgrc", MetadataFieldType.STRING)
			);

			ConditionAndExpression avamar = new ConditionAndExpression(
				new SimpleCondition(
					new FieldExpression("Backupjob", "module"),
					Operator.EQUALS,
					new ConstantExpression("AvamarModule", MetadataFieldType.STRING)
				),
				new ConstantExpression("avagent", MetadataFieldType.STRING)
			);

			Set<ConditionAndExpression> possibilities = new HashSet<>();
			possibilities.add(netbackup);
			possibilities.add(networker);
			possibilities.add(backupExec);
			possibilities.add(dataProtector);
			possibilities.add(tsm);
			possibilities.add(arcServe);
			possibilities.add(commVault);
			possibilities.add(avamar);

			Condition fetchProcessStatus = new CompoundCondition(
				new SimpleCondition(new FieldExpression("ProcessStatus", "nodeId"),
					Operator.EQUALS, new FieldExpression("Host", "id")),
				new SimpleCondition(new FieldExpression("ProcessStatus", "name"),
					Operator.CONTAINS, new ConditionalExpression(MetadataFieldType.STRING, possibilities)),
				LogicalOperator.AND
			);

			Set<Condition> fetchConditions = new HashSet<>();
			fetchConditions.add(fetchHostStatus);
			fetchConditions.add(fetchProcessStatus);

			Condition condition = new CompoundCondition(
				new SimpleCondition(
					new CalculationExpression(DataManipulationFunction.COUNT, "HostStatus", "id"),
					Operator.GREATER_THAN,
					new ConstantExpression("0", MetadataFieldType.NUMERIC)
				),
				new SimpleCondition(
					new CalculationExpression(DataManipulationFunction.COUNT, "ProcessStatus", "id"),
					Operator.EQUALS,
					new ConstantExpression("0", MetadataFieldType.NUMERIC)
				),
				LogicalOperator.AND
			);

			Cause noAgentOnClient = new Cause(
				NO_AGENT_RAN_ON_CLIENT, "Host", MappingRelationType.JOB_BACKING_UP_HOST,
				fetchConditions, condition, "Backup agent was down on host: {param}", DataManipulationFunction.LAST,
				messageArgs1, null);

			return new WeightedCause(noAgentOnClient, 100);
		}

		private static WeightedCause buildNoAgentOnClient2() {

			Expression agentName = new FieldExpression("BackupClient", "name");
			NumberedExpression agentFirst = new NumberedExpression(1L, agentName);
			Set<NumberedExpression> messageArgs1 = new HashSet<>();
			messageArgs1.add(agentFirst);

			Condition fetchCondition6 = new SimpleCondition(new FieldExpression("ClientStatus", "nodeId"),
				Operator.EQUALS, new FieldExpression("BackupClient", "id"));

			Set<Condition> fetchConditions6 = new HashSet<>();
			fetchConditions6.add(fetchCondition6);


			Cause AgentDown = new Cause(BACKUP_AGENT_IS_DOWN_ON_BACKUP_CLIENT, "BackupClient", MappingRelationType.JOB_USES_BACKUPCLIENT,
				fetchConditions6,
				new CompoundCondition(
					new SimpleCondition(new FieldExpression("ClientStatus", "responding"), Operator.EQUALS, new ConstantExpression("true", MetadataFieldType.BOOLEAN)),
					new SimpleCondition(new FieldExpression("ClientStatus", "daemonRunning"), Operator.EQUALS, new ConstantExpression("false", MetadataFieldType.BOOLEAN))),
				"Backup agent {param} is down on backup client", DataManipulationFunction.LAST, messageArgs1, null);

			return new WeightedCause(AgentDown, 101);
		}

		private static WeightedCause buildPortSwitchForEthernet() {
			Expression portName = new FieldExpression("EthernetPort", "name");
			Expression nodeType = new FieldExpression("NetintStatus", "module");
			Expression nodeName = new FieldExpression("NetintStatus", "agentName");
			NumberedExpression portNameFirst = new NumberedExpression(1L, portName);
			NumberedExpression nodeTypeSecond = new NumberedExpression(2L, nodeType);
			NumberedExpression nodeNameThird = new NumberedExpression(3L, nodeName);
			Set<NumberedExpression> messageArgs1 = new HashSet<>();
			messageArgs1.add(portNameFirst);
			messageArgs1.add(nodeTypeSecond);
			messageArgs1.add(nodeNameThird);

			Condition fetchCondition7 = new SimpleCondition(new FieldExpression("NetintStatus", "nodeId"),
				Operator.EQUALS, new FieldExpression("EthernetPort", "id"));

			Set<Condition> fetchConditions7 = new HashSet<>();
			fetchConditions7.add(fetchCondition7);


			Cause SwitchPortDown = new Cause("Switch port is down for ethernet", "EthernetPort", null,
				fetchConditions7,
				new SimpleCondition(new FieldExpression("NetintStatus", "linkup"), Operator.EQUALS, new ConstantExpression("false", MetadataFieldType.BOOLEAN)),
				"Ethernet port:{param} for {param}:{param} is down", DataManipulationFunction.EXISTS, messageArgs1, "com.emc.dpa.analysis.rca.RCAPortSwitchIsDownPlugin");

			return new WeightedCause(SwitchPortDown, 61);
		}


		private static WeightedCause buildDifferentPortSettings() {
			Set<NumberedExpression> messageArgs1 = new HashSet<>();

			Condition fetchCondition1 = new SimpleCondition(new FieldExpression("NetintStatus", "nodeId"),
				Operator.EQUALS, new FieldExpression("EthernetPort", "id"));

			Condition fetchCondition2 = new SimpleCondition(new FieldExpression("NetintConfig", "nodeId"),
				Operator.EQUALS, new FieldExpression("EthernetPort", "id"));

			Set<Condition> fetchConditions = new HashSet<>();
			fetchConditions.add(fetchCondition1);
			fetchConditions.add(fetchCondition2);

			Cause PortSettings = new Cause("Different Port Settings", "EthernetPort", null,
				fetchConditions,
				new SimpleCondition(new FieldExpression("NetintStatus", "speed"), Operator.NOT_EQUALS,
					new ConstantExpression("0", MetadataFieldType.NUMERIC)),
				"", DataManipulationFunction.EXISTS, messageArgs1, "com.emc.dpa.analysis.rca.RCASwitchSettingsPlugin");

			return new WeightedCause(PortSettings, 63);
		}


		private static WeightedCause buildPortSwitchForFibreChannel() {
			Expression portName = new FieldExpression("FibreChannelPort", "name");
			Expression nodeType = new FieldExpression("FcportStatus", "module");
			Expression nodeName = new FieldExpression("FcportStatus", "agentName");
			NumberedExpression portNameFirst = new NumberedExpression(1L, portName);
			NumberedExpression nodeTypeSecond = new NumberedExpression(2L, nodeType);
			NumberedExpression nodeNameThird = new NumberedExpression(3L, nodeName);
			Set<NumberedExpression> messageArgs1 = new HashSet<>();
			messageArgs1.add(portNameFirst);
			messageArgs1.add(nodeTypeSecond);
			messageArgs1.add(nodeNameThird);

			Condition fetchCondition7 = new SimpleCondition(new FieldExpression("FcportStatus", "nodeId"),
				Operator.EQUALS, new FieldExpression("FibreChannelPort", "id"));

			Set<Condition> fetchConditions7 = new HashSet<>();
			fetchConditions7.add(fetchCondition7);


			Cause SwitchPortDown = new Cause("Switch port is down for fibre channel", "FibreChannelPort", null,
				fetchConditions7,
				new SimpleCondition(new FieldExpression("FcportStatus", "linkup"), Operator.EQUALS, new ConstantExpression("false", MetadataFieldType.BOOLEAN)),
				"Fibre channel port:{param} for {param}:{param} is down", DataManipulationFunction.EXISTS, messageArgs1, "com.emc.dpa.analysis.rca.RCAPortSwitchIsDownPlugin");

			return new WeightedCause(SwitchPortDown, 60);

		}


		private static RCATemplate buildRPAHighLoadTemplate() {
			Symptom symptom = new Symptom("CG Copy high load", "CgCopyPerf",
				new SimpleCondition(new FieldExpression("CgCopyPerf", "highLoadTime"), Operator.GREATER_THAN, new ConstantExpression("0", MetadataFieldType.NUMERIC)));
			symptom.setFilterString("highLoadTime > 0");
			symptom.setNodeTypeString("cg copies");
			symptom.setProcessTypeString("replications");


			WeightedCause cause1 = buildHighRPAThroughput();
			WeightedCause cause2 = buildCGCopyFF();
			WeightedCause cause3 = buildPacketLoss();

			Set<WeightedCause> causes = new HashSet<>();
			causes.add(cause1);
			causes.add(cause2);
			causes.add(cause3);

			return new RCATemplate("CG copy High load template", symptom, causes);
		}

		private static WeightedCause buildHighRPAThroughput() {

			ParameterExpression highThroughputThreshold = new ParameterExpression("highThroughputThreshold", "122880", MetadataFieldType.NUMERIC);
			ConstantExpression megaSize = new ConstantExpression("1024", MetadataFieldType.NUMERIC);

			Expression agentName = new FieldExpression("RpaPerfView", "agentName");
			NumberedExpression rpaFirst = new NumberedExpression(1L, agentName);
			Expression rpaName = new FieldExpression("AbstractRecoverPointAppliance", "name");
			NumberedExpression rpaSecond = new NumberedExpression(2L, rpaName);
			Expression site = new FieldExpression("RpaPerfView", "site");
			NumberedExpression rpaThird = new NumberedExpression(3L, site);

			MathExpression multiplyWanCompression = new MathExpression(
				new FieldExpression("RpaPerfView", "wanThroughput"),
				new FieldExpression("RpaPerfView", "compression"),
				MathOperator.MULTIPLY);

			Expression wanThroughput = new MathExpression(new FieldExpression("RpaPerfView", "wanThroughput"), multiplyWanCompression, MathOperator.MAX);
			Expression maxThroughput = new MathExpression(new FieldExpression("RpaPerfView", "sanThroughput"), wanThroughput, MathOperator.MAX);
			Expression maxThroughputMB = new MathExpression(maxThroughput, megaSize, MathOperator.DIVIDE);
			NumberedExpression rpaFourth = new NumberedExpression(4L, maxThroughputMB);
			Expression limitThroughputMB = new MathExpression(highThroughputThreshold, megaSize, MathOperator.DIVIDE);
			NumberedExpression rpaFifth = new NumberedExpression(5L, limitThroughputMB);
			Set<NumberedExpression> messageArgs1 = new HashSet<>();
			messageArgs1.add(rpaFirst);
			messageArgs1.add(rpaSecond);
			messageArgs1.add(rpaThird);
			messageArgs1.add(rpaFourth);
			messageArgs1.add(rpaFifth);

			Condition fetchCondition = new SimpleCondition(new FieldExpression("RpaPerfView", "nodeId"),
				Operator.EQUALS, new FieldExpression("AbstractRecoverPointAppliance", "id"));
			Set<Condition> fetchConditions = new HashSet<>();
			fetchConditions.add(fetchCondition);


			///(san != null and san>120) || (wan != null and (((compression = null || compression = 0) and wan > 120) || ((compression != null and compression != 0) and wan*compression > 120)))
			Condition condition = new CompoundCondition(
				new CompoundCondition(
					new SimpleCondition(new FieldExpression("RpaPerfView", "sanThroughput"), Operator.NOT_EQUALS, new ConstantExpression("null", MetadataFieldType.NULL)),
					new SimpleCondition(new FieldExpression("RpaPerfView", "sanThroughput"), Operator.GREATER_THAN, highThroughputThreshold),
					LogicalOperator.AND),
				new CompoundCondition(
					new SimpleCondition(new FieldExpression("RpaPerfView", "wanThroughput"), Operator.NOT_EQUALS, new ConstantExpression("null", MetadataFieldType.NULL)),
					new CompoundCondition(
						new CompoundCondition(
							new SimpleCondition(new FieldExpression("RpaPerfView", "wanThroughput"), Operator.GREATER_THAN, highThroughputThreshold),
							new CompoundCondition(
								new SimpleCondition(new FieldExpression("RpaPerfView", "compression"), Operator.EQUALS, new ConstantExpression("null", MetadataFieldType.NULL)),
								new SimpleCondition(new FieldExpression("RpaPerfView", "compression"), Operator.EQUALS, new ConstantExpression("0", MetadataFieldType.NUMERIC)),
								LogicalOperator.OR),
							LogicalOperator.AND),
						new CompoundCondition(
							new CompoundCondition(
								new SimpleCondition(new FieldExpression("RpaPerfView", "compression"), Operator.NOT_EQUALS, new ConstantExpression("null", MetadataFieldType.NULL)),
								new SimpleCondition(new FieldExpression("RpaPerfView", "compression"), Operator.NOT_EQUALS, new ConstantExpression("0", MetadataFieldType.NUMERIC)),
								LogicalOperator.AND),
							new SimpleCondition(multiplyWanCompression, Operator.GREATER_THAN, highThroughputThreshold),
							LogicalOperator.AND),
						LogicalOperator.OR),
					LogicalOperator.AND),
				LogicalOperator.OR);

			Cause rpaThroughputHigh = new Cause("High RPA throughput", "AbstractRecoverPointAppliance", MappingRelationType.RECOVERPOINT_CG_COPY_ACTIVE_RPA,
				fetchConditions, condition, "The throughput for RPA: {param}:{param} at site {param} was {param}MB, above the RPA throughput limitation ({param}MB)", DataManipulationFunction.EXISTS,
				messageArgs1, null);

			TimeManipulation startTimeManipulation = new TimeManipulation(
				ManipulationType.MOVE_IT_BY_ABSOLUTE_TIME,
				MathOperator.SUBTRACT,
				5 * 60L,
				null,
				"rpa_throughput_offset"
			);
			rpaThroughputHigh.setStartTimeManipulation(startTimeManipulation);

			rpaThroughputHigh.setGeneralMessageTemplate("The throughput for RPA: {param}:{param} at site {param} was above the RPA throughput limitation ({param}MB)");
			Set<NumberedExpression> generalMessageParams = new HashSet<>();
			generalMessageParams.add(rpaFirst);
			generalMessageParams.add(rpaSecond);
			generalMessageParams.add(rpaThird);
			NumberedExpression rpaFifthWichIsNowForth = new NumberedExpression(4L, new MathExpression(highThroughputThreshold, megaSize, MathOperator.DIVIDE));
			generalMessageParams.add(rpaFifthWichIsNowForth);
			rpaThroughputHigh.setGeneralMessageTemplateParams(generalMessageParams);

			return new WeightedCause(rpaThroughputHigh, 100);
		}

		private static WeightedCause buildCGCopyFF() {
			Expression cgCopyName = new FieldExpression("AbstractRecoverPointConsistencyGroupCopy", "name");
			NumberedExpression cgCopyFirst = new NumberedExpression(1L, cgCopyName);
			Set<NumberedExpression> messageArgs1 = new HashSet<>();
			messageArgs1.add(cgCopyFirst);

			Condition fetchCondition = new SimpleCondition(new FieldExpression("CgCopyStatus", "nodeId"),
				Operator.EQUALS, new FieldExpression("AbstractRecoverPointConsistencyGroupCopy", "id"));
			Set<Condition> fetchConditions = new HashSet<>();
			fetchConditions.add(fetchCondition);

			Condition condition =
				new CompoundCondition(
					new SimpleCondition(new FieldExpression("CgCopyStatus", "journalMode"), Operator.NOT_EQUALS, new ConstantExpression("null", MetadataFieldType.NULL)),
					new SimpleCondition(new FieldExpression("CgCopyStatus", "journalMode"), Operator.EQUALS, new ConstantExpression("Fast Forward", MetadataFieldType.STRING)),
					LogicalOperator.AND);

			Cause rpaFastForward = new Cause("High RPA throughput", "AbstractRecoverPointConsistencyGroupCopy", null,
				fetchConditions, condition, "Cg Copy {param} has a high latency on writing to remote storage (Fast Forward)", DataManipulationFunction.EXISTS,
				messageArgs1, null);

			return new WeightedCause(rpaFastForward, 80);
		}

		private static RCATemplate buildRPOViolationTemplate() {
			Symptom symptom = new Symptom(CG_COPY_RPO_VIOLATION_SYMPTOM_NAME, "AnalysisAlert",
				new CompoundCondition(
					// todo - check state - not closed, currently don't bother
					new SimpleCondition(new FieldExpression("AnalysisAlert", "message"), Operator.EQUALS, new ConstantExpression("RPO Violation", MetadataFieldType.STRING)),
					new SimpleCondition(new FieldExpression("AnalysisAlert", "component"), Operator.CONTAINS, new ConstantExpression("CG Copy", MetadataFieldType.STRING))
				)
			);
			symptom.setFilterString("message = 'RPO Violation' AND component like '%CG Copy%'");
			symptom.setNodeTypeString("cg copies");
			symptom.setProcessTypeString("replications");


			WeightedCause cause1 = buildHighRPAThroughput();
			WeightedCause cause2 = buildCGCopyFF();
			WeightedCause cause3 = buildPacketLoss();

			Set<WeightedCause> causes = new HashSet<>();
			causes.add(cause1);
			causes.add(cause2);
			causes.add(cause3);

			return new RCATemplate("CG copy RPO Violation template", symptom, causes);
		}

		private static WeightedCause buildPacketLoss() {

			Expression packetLossSize = new FieldExpression("RpaPerfView", "packetLoss");
			NumberedExpression clientFirst = new NumberedExpression(1L, packetLossSize);
			Set<NumberedExpression> messageArgs1 = new HashSet<>();
			messageArgs1.add(clientFirst);

			Condition fetchCondition = new SimpleCondition(new FieldExpression("RpaPerfView", "nodeId"),
				Operator.EQUALS, new FieldExpression("AbstractRecoverPointAppliance", "id"));
			Set<Condition> fetchConditions = new HashSet<>();
			fetchConditions.add(fetchCondition);
			Condition condition = new SimpleCondition(new FieldExpression("RpaPerfView", "packetLoss"), Operator.GREATER_THAN,
				new ConstantExpression("1", MetadataFieldType.NUMERIC));

			Cause packetLoss = new Cause(PACKET_LOSS, "AbstractRecoverPointAppliance", MappingRelationType.RECOVERPOINT_CG_COPY_ACTIVE_RPA,
				fetchConditions, condition,
				"There is a bottleneck on the bandwidth. Found packet-loss ({param}) on the link.", DataManipulationFunction.EXISTS,
				messageArgs1, null);

			packetLoss.setGeneralMessageTemplate("There is a bottleneck on the bandwidth. Found packet-loss (>1) on the link.");

			return new WeightedCause(packetLoss, 200);
		}

		public static List<RCATemplate> getDefaultRCATemplates() {
			return Arrays.asList(buildBackupFailedTemplate(), buildRPAHighLoadTemplate(), buildRPOViolationTemplate());
		}

	}

}
