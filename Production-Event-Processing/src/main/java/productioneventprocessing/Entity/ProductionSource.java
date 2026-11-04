package productioneventprocessing.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "production_sources")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductionSource {
    @Id
    @Column(name = "source_id", length = 64)
    private String sourceId;

    @Column(name = "display_name", length = 255)
    private String displayName;
}
