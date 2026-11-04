package productioneventprocessing.Repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import productioneventprocessing.Entity.EventStatus;
import productioneventprocessing.Entity.EventType;
import productioneventprocessing.Entity.ProductionEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductionEventRepository extends JpaRepository<ProductionEvent, UUID> {
    Optional<ProductionEvent> findByEventId(String eventId);

    Optional<ProductionEvent> findByEventIdAndSourceId(String eventId, String sourceId);

    List<ProductionEvent> findBySourceIdAndTypeAndStatusAndTargetEventId(
            String sourceId, EventType type, EventStatus status, String targetEventId);

    /* ---------- Summary queries ---------- */

    @Query("""
        SELECT COALESCE(SUM(e.quantity), 0)
        FROM ProductionEvent e
        WHERE e.type = :countType
          AND e.status = :processedStatus
          AND NOT EXISTS (
              SELECT 1 FROM ProductionEvent v
              WHERE v.type = :voidType
                AND v.status = :processedStatus
                AND v.targetEventId = e.eventId
                AND v.sourceId = e.sourceId
          )
    """)
    long netTotalAll(@Param("countType") EventType countType,
                     @Param("voidType") EventType voidType,
                     @Param("processedStatus") EventStatus processedStatus);

    @Query("""
        SELECT COALESCE(SUM(e.quantity), 0)
        FROM ProductionEvent e
        WHERE e.sourceId = :sourceId
          AND e.type = :countType
          AND e.status = :processedStatus
          AND NOT EXISTS (
              SELECT 1 FROM ProductionEvent v
              WHERE v.type = :voidType
                AND v.status = :processedStatus
                AND v.targetEventId = e.eventId
                AND v.sourceId = e.sourceId
          )
    """)
    long netTotalForSource(@Param("sourceId") String sourceId,
                           @Param("countType") EventType countType,
                           @Param("voidType") EventType voidType,
                           @Param("processedStatus") EventStatus processedStatus);

    @Query("SELECT COUNT(e) FROM ProductionEvent e WHERE e.status = :status")
    long countByStatus(@Param("status") EventStatus status);

    @Query("SELECT COUNT(e) FROM ProductionEvent e WHERE e.sourceId = :sourceId AND e.status = :status")
    long countBySourceAndStatus(@Param("sourceId") String sourceId,
                                @Param("status") EventStatus status);

    @Query("SELECT COUNT(e) FROM ProductionEvent e " +
            "WHERE e.type = :countType AND e.status = :status AND e.acknowledgedAt IS NULL")
    long pendingAckAll(@Param("countType") EventType countType,
                       @Param("status") EventStatus status);

    @Query("SELECT COUNT(e) FROM ProductionEvent e " +
            "WHERE e.sourceId = :sourceId AND e.type = :countType " +
            "AND e.status = :status AND e.acknowledgedAt IS NULL")
    long pendingAckForSource(@Param("sourceId") String sourceId,
                             @Param("countType") EventType countType,
                             @Param("status") EventStatus status);

    /* ---------- Pending view ---------- */

    @Query("""
        SELECT e FROM ProductionEvent e
        WHERE e.type = :countType AND e.status = :status AND e.acknowledgedAt IS NULL
        ORDER BY e.eventTime ASC
    """)
    List<ProductionEvent> findPendingAll(@Param("countType") EventType countType,
                                         @Param("status") EventStatus status);

    @Query("""
        SELECT e FROM ProductionEvent e
        WHERE e.sourceId = :sourceId AND e.type = :countType
          AND e.status = :status AND e.acknowledgedAt IS NULL
        ORDER BY e.eventTime ASC
    """)
    List<ProductionEvent> findPendingForSource(@Param("sourceId") String sourceId,
                                               @Param("countType") EventType countType,
                                               @Param("status") EventStatus status);

    /* ---------- Exceptions view (PENDING_REFERENCE) ---------- */

    @Query("SELECT e FROM ProductionEvent e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<ProductionEvent> findByStatus(@Param("status") EventStatus status);

    @Query("SELECT e FROM ProductionEvent e WHERE e.sourceId = :sourceId AND e.status = :status ORDER BY e.createdAt ASC")
    List<ProductionEvent> findBySourceAndStatus(@Param("sourceId") String sourceId,
                                                @Param("status") EventStatus status);

    /* ---------- Pessimistic lock for ack ---------- */

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM ProductionEvent e WHERE e.eventId = :eventId")
    Optional<ProductionEvent> findAndLockByEventId(@Param("eventId") String eventId);
}
