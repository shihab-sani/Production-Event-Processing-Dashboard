package productioneventprocessing.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import productioneventprocessing.Entity.ProductionSource;

public interface ProductionSourceRepository extends JpaRepository<ProductionSource, String> {
    @Modifying
    @Query(value = "INSERT INTO production_sources (source_id, display_name) " +
            "VALUES (:sid, :sid) ON CONFLICT (source_id) DO NOTHING",
            nativeQuery = true)
    void upsert(@Param("sid") String sourceId);
}
