@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula(
    "case when debitKey is not null " +
    "then 'Debit' " +
    "else ( " +
    "   case when creditKey is not null " +
    "   then 'Credit' " +
    "   else 'Unknown' " +
    "   end ) " +
    "end "
)
public class Account {

    @Id
    private Long id;

    private String owner;

    private BigDecimal balance;

    private BigDecimal interestRate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }
}

@Entity
@DiscriminatorValue(value = "Debit")
public class DebitAccount extends Account {

    private String debitKey;

    private BigDecimal overdraftFee;

    private DebitAccount() {}

    public DebitAccount(String debitKey) {
        this.debitKey = debitKey;
    }

    public String getDebitKey() {
        return debitKey;
    }

    public BigDecimal getOverdraftFee() {
        return overdraftFee;
    }

    public void setOverdraftFee(BigDecimal overdraftFee) {
        this.overdraftFee = overdraftFee;
    }
}

@Entity
@DiscriminatorValue(value = "Credit")
public static class CreditAccount extends Account {

    private String creditKey;

    private BigDecimal creditLimit;

    private CreditAccount() {}

    public CreditAccount(String creditKey) {
        this.creditKey = creditKey;
    }

    public String getCreditKey() {
        return creditKey;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }
}