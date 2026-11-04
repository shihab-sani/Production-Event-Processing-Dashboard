package productioneventprocessing.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import productioneventprocessing.Entity.SubmissionAttempt;

import java.util.List;

public interface SubmissionAttemptRepository extends JpaRepository<SubmissionAttempt,Long> {
    @Query("SELECT COUNT(a) FROM SubmissionAttempt a WHERE a.classification = :cls")
    long countByClassification(@Param("cls") String classification);

    @Query("SELECT COUNT(a) FROM SubmissionAttempt a WHERE a.classification = :cls AND a.sourceId = :sourceId")
    long countByClassificationAndSource(@Param("cls") String classification,
                                        @Param("sourceId") String sourceId);

    List<SubmissionAttempt> findByClassificationIn(List<String> classes);

    List<SubmissionAttempt> findByClassificationInAndSourceId(List<String> classes, String sourceId);
}
