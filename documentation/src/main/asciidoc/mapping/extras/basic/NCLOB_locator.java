@Entity
public class Product {
	...
	@Lob
	@Basic
	@Nationalized
	public NClob description;
	// Clob also works, because NClob
	// extends Clob.  The db type is
	// still NCLOB either way and
	// handled as such
}